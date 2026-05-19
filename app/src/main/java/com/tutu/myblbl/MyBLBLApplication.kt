package com.tutu.myblbl

import android.app.Application
import android.os.SystemClock
import android.view.View
import androidx.asynclayoutinflater.view.AsyncLayoutInflater
import androidx.appcompat.view.ContextThemeWrapper
import com.tutu.myblbl.core.common.log.AppLog
import com.tutu.myblbl.core.common.settings.AppSettingsDataStore
import com.tutu.myblbl.core.lifecycle.AppBackgroundMonitor
import com.tutu.myblbl.core.ui.base.BaseActivity
import com.tutu.myblbl.core.ui.image.ImageLoader
import com.tutu.myblbl.core.ui.image.MyBLBLCoilInitializer
import com.tutu.myblbl.di.appModules
import com.tutu.myblbl.feature.home.RecommendFeedRepository
import com.tutu.myblbl.feature.player.PlayerInstancePool
import com.tutu.myblbl.network.NetworkManager
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
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

    // 预 inflate 出来的 activity_main root view（含 view_tab_bar 嵌套 inflate）。
    // 使用 AtomicReference 是因为生产/消费跨线程（AsyncLayoutInflater worker thread → MainActivity onCreate main thread）。
    // 消费 (getAndSet null) 后即使 Activity recreate 也会走原始 inflate 路径，避免错用旧的 detached view。
    private val preInflatedActivityMain = AtomicReference<View?>(null)
    private val preInflateScheduled = AtomicBoolean(false)
    
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
        trace("initCoil", startMs) { MyBLBLCoilInitializer.bootstrap(this) }
        // 提前算好图片质量等级缓存，让首屏 RecyclerView bind 时零 DI 查询
        ImageLoader.prewarm()
        trace("initBackgroundMonitor", startMs) { AppBackgroundMonitor.init(this) }
        // Application onCreate 末尾立刻 schedule activity_main 预 inflate：
        // - AsyncLayoutInflater 在它自己的 worker thread 跑，不阻塞主线程；
        // - 主线程接下来还要走 ActivityThread.handleLaunchActivity → MainActivity.onCreate，
        //   中间通常 100~300ms（Activity Token、Window 注册、theme apply 等系统侧工作），
        //   足够 inflate 完成（实测 activity_main + view_tab_bar 嵌套 inflate 约 80~150ms）。
        schedulePreInflateActivityMain()
        AppLog.i(TAG, "STARTUP T1 app.onCreate end elapsed=${SystemClock.elapsedRealtime() - startMs}ms")
    }

    private fun schedulePreInflateActivityMain() {
        if (!preInflateScheduled.compareAndSet(false, true)) return
        val themeIndex = runCatching {
            KoinPlatform.getKoin().get<AppSettingsDataStore>().getCachedInt("theme", 1)
        }.getOrDefault(1)
        // 用 ContextThemeWrapper 确保子 view 的 ?attr/xxx 能解析到与 MainActivity.applyTheme 一致的 theme。
        val themedContext = ContextThemeWrapper(this, BaseActivity.themeIndexToResId(themeIndex))
        AsyncLayoutInflater(themedContext).inflate(R.layout.activity_main, null) { view, _, _ ->
            preInflatedActivityMain.set(view)
            AppLog.i(TAG, "STARTUP activity_main pre-inflated")
        }
    }

    /**
     * MainActivity.getViewBinding 调用一次：拿到预 inflate 的 view 后置空（避免 recreate 复用 stale view）。
     */
    fun consumePreInflatedActivityMain(): View? = preInflatedActivityMain.getAndSet(null)

    private inline fun trace(name: String, appStartMs: Long, block: () -> Unit) {
        val stepStartMs = SystemClock.elapsedRealtime()
        block()
        AppLog.i(
            TAG,
            "$name end step=${SystemClock.elapsedRealtime() - stepStartMs}ms total=${SystemClock.elapsedRealtime() - appStartMs}ms"
        )
    }

    private fun initSettings() {
        KoinPlatform.getKoin().get<AppSettingsDataStore>().initCache()
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
            if (NetworkManager.isLoggedIn()) {
                NetworkManager.prewarmWebSession()
            } else {
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
