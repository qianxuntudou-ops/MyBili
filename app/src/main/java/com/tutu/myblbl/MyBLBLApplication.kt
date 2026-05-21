package com.tutu.myblbl

import android.app.Application
import android.os.SystemClock
import com.tutu.myblbl.core.common.log.AppLog
import com.tutu.myblbl.core.common.settings.AppSettingsDataStore
import com.tutu.myblbl.core.lifecycle.AppBackgroundMonitor
import com.tutu.myblbl.core.ui.image.ImageLoader
import com.tutu.myblbl.di.appModules
import com.tutu.myblbl.feature.home.RecommendFeedRepository
import com.tutu.myblbl.feature.player.PlayerInstancePool
import com.tutu.myblbl.network.NetworkManager
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import org.koin.mp.KoinPlatform

class MyBLBLApplication : Application() {

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val startupPrewarmScheduled = AtomicBoolean(false)
    
    companion object {
        private const val TAG = "AppStartup"

        lateinit var instance: MyBLBLApplication
            private set
    }
    
    override fun onCreate() {
        val startMs = SystemClock.elapsedRealtime()
        AppLog.i(TAG, "STARTUP T0 app.onCreate start")
        super.onCreate()
        instance = this
        AppLog.init(this)

        // Koin 必须先初始化（后续所有组件依赖 DI）
        trace("initKoin", startMs) { initKoin() }
        // Settings 必须在 Network 之前：CookieManager.loadCookiesFromPrefs 依赖 AppSettingsDataStore cache
        trace("initSettings", startMs) { initSettings() }
        trace("initNetwork", startMs) { initNetwork() }
        // 图片质量预热：提前缓存质量等级，让首屏 RecyclerView bind 时零 DI 查询
        ImageLoader.prewarm()
        trace("initBackgroundMonitor", startMs) { AppBackgroundMonitor.init(this) }
        AppLog.i(TAG, "STARTUP T1 app.onCreate end elapsed=${SystemClock.elapsedRealtime() - startMs}ms")
    }

    private inline fun trace(name: String, appStartMs: Long, block: () -> Unit) {
        val stepStartMs = SystemClock.elapsedRealtime()
        block()
        AppLog.i(
            TAG,
            "$name end step=${SystemClock.elapsedRealtime() - stepStartMs}ms total=${SystemClock.elapsedRealtime() - appStartMs}ms"
        )
    }

    private fun initSettings() {
        KoinPlatform.getKoin().get<AppSettingsDataStore>().initCacheBlocking("application_init")
    }
    
    private fun initKoin() {
        startKoin {
            androidLogger(if (BuildConfig.DEBUG) Level.ERROR else Level.NONE)
            androidContext(this@MyBLBLApplication)
            modules(appModules)
        }
    }
    
    private fun initNetwork() {
        NetworkManager.init(this, syncWebViewCookies = false)
        // 不再做 api.bilibili.com 预热：实测 preheat 完成比 preloadFirstPage 的 API 返回还晚
        // （两个 appScope.launch 并发启动，preheat 自己也要走完整套 interceptor 链，
        // 拿到 connection 时 preloadFirstPage 已经先抢到了），收益为 0 反而多一次无效 HEAD。
        appScope.launch {
            // 推荐首页只依赖本地持久化的 cookies。CookieJar 在 NetworkManager.init 阶段
            // 已经从 SP 加载完毕（initSettings 同步阻塞保证 cache 就绪），此处直接发请求即可。
            // 不再做 WebView cookie 同步：webCookieManager 里的 cookie 全部来自 OkHttp
            // saveFromResponse 反向写入，跟我们的 SP 同源，冷启动期没有任何"新 cookie"
            // 可同步；而 WebView 冷加载在电视上 300~800ms，是纯负担。
            if (NetworkManager.isLoggedIn()) {
                KoinPlatform.getKoin().get<com.tutu.myblbl.event.AppEventHub>()
                    .dispatch(com.tutu.myblbl.event.AppEventHub.Event.UserSessionChanged)
            }
            runCatching {
                KoinPlatform.getKoin().get<RecommendFeedRepository>().preloadFirstPage()
            }
        }
    }

    fun scheduleDeferredSessionPrewarm(delayMillis: Long = 300L) {
        if (!startupPrewarmScheduled.compareAndSet(false, true)) {
            return
        }
        appScope.launch {
            if (delayMillis > 0) {
                delay(delayMillis)
            }
            val startMs = SystemClock.elapsedRealtime()
            if (NetworkManager.isLoggedIn()) {
                AppLog.i(TAG, "STARTUP sessionPrewarm start")
                val success = NetworkManager.prewarmWebSession()
                AppLog.i(
                    TAG,
                    "STARTUP sessionPrewarm end success=$success elapsed=${SystemClock.elapsedRealtime() - startMs}ms"
                )
            } else {
                AppLog.i(TAG, "STARTUP sessionPrewarm skip loggedOut")
                startupPrewarmScheduled.set(false)
            }
        }
    }

    override fun onTrimMemory(level: Int) {
        super.onTrimMemory(level)
        when {
            level >= 80 -> { // TRIM_MEMORY_COMPLETE
                ImageLoader.clearMemory(this)
                if (!PlayerInstancePool.isAttached()) {
                    PlayerInstancePool.releaseNow("trimMemory_complete")
                }
            }
            level >= 60 -> { // TRIM_MEMORY_MODERATE
                ImageLoader.clearMemory(this)
            }
            level >= 10 -> { // TRIM_MEMORY_RUNNING_LOW
                if (!PlayerInstancePool.isAttached()) {
                    PlayerInstancePool.releaseNow("trimMemory_running_low")
                }
            }
        }
    }
}
