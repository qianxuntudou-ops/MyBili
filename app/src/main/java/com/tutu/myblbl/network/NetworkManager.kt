package com.tutu.myblbl.network

import android.content.Context
import com.google.gson.GsonBuilder
import com.tutu.myblbl.BuildConfig
import com.tutu.myblbl.model.BaseResponse
import com.tutu.myblbl.model.adapter.FlexibleIntAdapter
import com.tutu.myblbl.model.adapter.FlexibleLongAdapter
import com.tutu.myblbl.model.user.UserDetailInfoModel
import com.tutu.myblbl.network.interceptor.HeaderInterceptor
import com.tutu.myblbl.network.interceptor.HttpCacheInterceptor
import com.tutu.myblbl.network.api.ApiService
import com.tutu.myblbl.network.response.Base2Response
import com.tutu.myblbl.network.response.BaseBaseResponse
import com.tutu.myblbl.utils.AppLog
import com.tutu.myblbl.utils.CookieManager
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.random.Random
import java.util.Locale

object NetworkManager {

    private const val TAG = "NetworkManager"
    
    private const val API_BASE = "https://api.bilibili.com/"
    private const val DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    private const val PREF_NAME = "app_settings"
    private const val KEY_CURRENT_UA = "currentUA"
    private const val PREWARM_INTERVAL_MS = 5 * 60 * 1000L
    private const val AUTH_INVALID_CODE = -101
    
    private var currentUA: String = DEFAULT_UA
    private var appContext: Context? = null
    
    private var wbiImageKey: String = ""
    private var wbiSubKey: String = ""
    
    private var userInfo: UserDetailInfoModel? = null
    private val prewarmMutex = Mutex()
    private var lastPrewarmTimestampMs: Long = 0L
    
    private val _cookieManager: CookieManager by lazy { CookieManager() }
    
    private val _okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .cookieJar(_cookieManager)
            .addInterceptor(HttpLoggingInterceptor().apply {
                level = if (BuildConfig.DEBUG) {
                    HttpLoggingInterceptor.Level.BASIC
                } else {
                    HttpLoggingInterceptor.Level.NONE
                }
            })
            .addInterceptor(HeaderInterceptor(
                userAgentProvider = { currentUA },
                acceptLanguageProvider = { getAcceptLanguage() }
            ))
            .addInterceptor(HttpCacheInterceptor(_cookieManager))
            .retryOnConnectionFailure(true)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .build()
    }
    
    private val gson by lazy {
        GsonBuilder()
            .registerTypeAdapter(Long::class.java, FlexibleLongAdapter())
            .registerTypeAdapter(java.lang.Long::class.java, FlexibleLongAdapter())
            .registerTypeAdapter(Int::class.java, FlexibleIntAdapter())
            .registerTypeAdapter(java.lang.Integer::class.java, FlexibleIntAdapter())
            .create()
    }

    private val retrofit: Retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(API_BASE)
            .client(_okHttpClient)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
    }
    
    val apiService: ApiService by lazy {
        retrofit.create(ApiService::class.java)
    }

    fun init(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext
        _cookieManager.init(applicationContext)
        currentUA = loadOrCreateCurrentUa(applicationContext)
        AppLog.d(
            TAG,
            "init: ua=${currentUA.take(80)}, hasSess=${_cookieManager.hasSessionCookie()}, identityCookies=${hasBaselineIdentityCookies()}"
        )
    }
    
    fun setWbiInfo(imgKey: String, subKey: String) {
        this.wbiImageKey = imgKey
        this.wbiSubKey = subKey
    }
    
    fun getWbiKeys(): Pair<String, String> {
        return Pair(wbiImageKey, wbiSubKey)
    }
    
    fun getCookieManager(): CookieManager = _cookieManager
    
    fun getCsrfToken(): String {
        return _cookieManager.getCsrfToken()
    }
    
    fun isLoggedIn(): Boolean {
        return _cookieManager.hasSessionCookie()
    }

    fun clearUserSession(clearCookies: Boolean = true, reason: String = "unknown") {
        if (clearCookies) {
            _cookieManager.clearCookies()
        }
        updateUserSession(null)
        lastPrewarmTimestampMs = 0L
        AppLog.d(TAG, "clearUserSession: clearCookies=$clearCookies reason=$reason")
    }

    fun getOkHttpClient(): OkHttpClient = _okHttpClient

    fun getCurrentUserAgent(): String = currentUA

    fun getAcceptLanguage(): String {
        val primaryLocale = Locale.getDefault()
        val languageTag = primaryLocale.toLanguageTag().ifBlank { "en-US" }
        val language = primaryLocale.language.ifBlank { "en" }
        return "$languageTag,$language;q=0.9,en-US;q=0.8,en;q=0.7"
    }

    fun refreshUserAgent(): String {
        val context = appContext
        val newUserAgent = generateDesktopUserAgent()
        currentUA = newUserAgent
        context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
            ?.edit()
            ?.putString(KEY_CURRENT_UA, newUserAgent)
            ?.apply()
        AppLog.d(TAG, "refreshUserAgent: ua=${newUserAgent.take(80)}")
        return newUserAgent
    }

    suspend fun prewarmWebSession(forceUaRefresh: Boolean = false): Boolean {
        return prewarmMutex.withLock {
            val now = System.currentTimeMillis()
            if (!forceUaRefresh && now - lastPrewarmTimestampMs < PREWARM_INTERVAL_MS && hasBaselineIdentityCookies()) {
                return@withLock true
            }
            _cookieManager.syncFromWebView()
            if (forceUaRefresh) {
                refreshUserAgent()
            }
            val mainPageLoaded = runCatching {
                apiService.getMainPage().use { body ->
                    body.source().request(1)
                }
            }.onFailure { throwable ->
                AppLog.e(TAG, "prewarmWebSession main page failed: ${throwable.message}", throwable)
            }.isSuccess
            val navSuccess = runCatching {
                val navResponse = apiService.getUserDetailInfo()
                syncUserSession(navResponse, source = "prewarmWebSession") != null
            }.onFailure { throwable ->
                AppLog.e(TAG, "prewarmWebSession nav failed: ${throwable.message}", throwable)
            }.getOrDefault(false)
            val success = mainPageLoaded || navSuccess
            if (mainPageLoaded) {
                _cookieManager.syncFromWebView()
            }
            if (success) {
                lastPrewarmTimestampMs = now
            }
            AppLog.d(
                TAG,
                "prewarmWebSession done: success=$success, mainPageLoaded=$mainPageLoaded, navSuccess=$navSuccess, forceUaRefresh=$forceUaRefresh, hasSess=${_cookieManager.hasSessionCookie()}, identityCookies=${hasBaselineIdentityCookies()}, ua=${currentUA.take(80)}"
            )
            success
        }
    }
    
    fun getUserInfo(): UserDetailInfoModel? {
        return userInfo
    }

    fun updateUserSession(info: UserDetailInfoModel?) {
        userInfo = info?.let {
            it.copy(face = normalizeAvatarUrl(it.face))
        }
        if (userInfo == null) {
            setWbiInfo("", "")
            return
        }
        val imgKey = userInfo!!.wbiImg?.imgUrl?.let(WbiGenerator::extractKeyFromUrl).orEmpty()
        val subKey = userInfo!!.wbiImg?.subUrl?.let(WbiGenerator::extractKeyFromUrl).orEmpty()
        setWbiInfo(imgKey, subKey)
    }

    private fun normalizeAvatarUrl(url: String): String = when {
        url.startsWith("http://") -> "https://${url.substring(7)}"
        url.startsWith("//") -> "https:$url"
        else -> url
    }

    fun syncUserSession(
        response: BaseResponse<UserDetailInfoModel>,
        source: String
    ): UserDetailInfoModel? {
        val info = response.data?.takeIf { response.isSuccess }
        if (info != null) {
            updateUserSession(info)
            return userInfo
        }
        if (isAuthInvalid(response.code)) {
            clearUserSession(reason = "$source code=${response.code}")
        }
        AppLog.d(
            TAG,
            "syncUserSession: source=$source code=${response.code} message=${response.errorMessage}"
        )
        return null
    }

    fun handleAuthFailureCode(code: Int, source: String) {
        if (isAuthInvalid(code)) {
            clearUserSession(reason = "$source code=$code")
        }
    }

    fun <T> syncAuthState(
        response: BaseResponse<T>,
        source: String
    ): BaseResponse<T> {
        handleAuthFailureCode(response.code, source)
        return response
    }

    fun syncAuthState(
        response: BaseBaseResponse,
        source: String
    ): BaseBaseResponse {
        handleAuthFailureCode(response.code, source)
        return response
    }

    fun <T> syncAuthState(
        response: Base2Response<T>,
        source: String
    ): Base2Response<T> {
        handleAuthFailureCode(response.code, source)
        return response
    }

    private fun loadOrCreateCurrentUa(context: Context): String {
        val prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        val stored = prefs.getString(KEY_CURRENT_UA, "").orEmpty().trim()
        if (stored.isNotBlank()) {
            return stored
        }
        val generated = generateDesktopUserAgent()
        prefs.edit().putString(KEY_CURRENT_UA, generated).apply()
        return generated
    }

    private fun generateDesktopUserAgent(): String {
        val chromeMajor = Random.nextInt(120, 135)
        val chromeBuild = Random.nextInt(0, 6500)
        val chromePatch = Random.nextInt(0, 220)
        return "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 " +
            "(KHTML, like Gecko) Chrome/$chromeMajor.0.$chromeBuild.$chromePatch Safari/537.36"
    }

    private fun hasBaselineIdentityCookies(): Boolean {
        val identityCookieNames = listOf(
            "buvid3",
            "buvid4",
            "b_nut",
            "_uuid",
            "b_lsid"
        )
        return identityCookieNames.any { cookieName ->
            !_cookieManager.getCookieValue(cookieName).isNullOrBlank()
        }
    }

    private fun isAuthInvalid(code: Int): Boolean {
        return code == AUTH_INVALID_CODE
    }
}
