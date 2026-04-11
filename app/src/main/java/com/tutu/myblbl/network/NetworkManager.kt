@file:Suppress("SpellCheckingInspection")

package com.tutu.myblbl.network

import android.content.Context
import androidx.core.content.edit
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
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import java.security.KeyFactory
import java.security.PublicKey
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import okhttp3.Cookie
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.FormBody
import org.json.JSONObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import android.util.Base64

object NetworkManager {

    private const val TAG = "NetworkManager"
    
    private const val API_BASE = "https://api.bilibili.com/"
    private const val DEFAULT_UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36"
    private const val PREF_NAME = "app_settings"
    private const val KEY_CURRENT_UA = "currentUA"
    private const val PREWARM_INTERVAL_MS = 5 * 60 * 1000L
    private const val AUTH_INVALID_CODE = -101
    private const val BILI_TICKET_KEY_ID = "ec02"
    private const val BILI_TICKET_HMAC_KEY = "XgwSnGZ1p"
    
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
            .registerTypeAdapter(Long::class.javaPrimitiveType, FlexibleLongAdapter())
            .registerTypeAdapter(Long::class.javaObjectType, FlexibleLongAdapter())
            .registerTypeAdapter(Int::class.javaPrimitiveType, FlexibleIntAdapter())
            .registerTypeAdapter(Int::class.javaObjectType, FlexibleIntAdapter())
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
        context?.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)?.edit {
            putString(KEY_CURRENT_UA, newUserAgent)
        }
        AppLog.d(TAG, "refreshUserAgent: ua=${newUserAgent.take(80)}")
        return newUserAgent
    }

    private var lastEnsureHealthyForPlayMs: Long = 0L
    private val ensureHealthyMutex = Mutex()

    suspend fun ensureHealthyForPlay() {
        ensureHealthyMutex.withLock {
            val now = System.currentTimeMillis()
            if (now - lastEnsureHealthyForPlayMs < 60_000L) return@withLock

            ensureWebFingerprintCookies()
            ensureBiliTicket()
            ensureBuvidActiveOncePerDay()
            refreshCookieIfNeededOncePerDay()

            lastEnsureHealthyForPlayMs = System.currentTimeMillis()
            AppLog.d(TAG, "ensureHealthyForPlay done")
        }
    }

    private suspend fun ensureWebFingerprintCookies() {
        val hasBuvid3 = !_cookieManager.getCookieValue("buvid3").isNullOrBlank()
        val hasBNut = !_cookieManager.getCookieValue("b_nut").isNullOrBlank()
        val hasBuvid4 = !_cookieManager.getCookieValue("buvid4").isNullOrBlank()

        if (!hasBuvid3 || !hasBNut) {
            runCatching {
                apiService.getMainPage().use { body ->
                    body.source().request(1)
                }
            }.onFailure {
                AppLog.w(TAG, "ensureWebFingerprintCookies homepage failed: ${it.message}")
            }
        }

        if (!hasBuvid4) {
            runCatching {
                val request = okhttp3.Request.Builder()
                    .url("https://api.bilibili.com/x/frontend/finger/spi")
                    .build()
                val response = _okHttpClient.newCall(request).execute()
                val body = response.body?.string().orEmpty()
                val json = JSONObject(body)
                val data = json.optJSONObject("data") ?: JSONObject()
                val b3 = data.optString("b_3", "").trim()
                val b4 = data.optString("b_4", "").trim()
                val expiresAt = System.currentTimeMillis() + 365L * 24 * 60 * 60 * 1000
                val cookies = mutableListOf<Cookie>()
                if (b3.isNotBlank() && _cookieManager.getCookieValue("buvid3").isNullOrBlank()) {
                    cookies.add(buildCookie("buvid3", b3, expiresAt))
                }
                if (b4.isNotBlank()) {
                    cookies.add(buildCookie("buvid4", b4, expiresAt))
                }
                if (cookies.isNotEmpty()) {
                    _cookieManager.saveCookies(cookies.map { encodeCookieDirect(it) })
                }
            }.onFailure {
                AppLog.w(TAG, "ensureWebFingerprintCookies spi failed: ${it.message}")
            }
        }
    }

    private var biliTicketCheckedDay: Long = 0L

    private suspend fun ensureBiliTicket() {
        val nowMs = System.currentTimeMillis()
        val epochDay = nowMs / 86_400_000L
        if (biliTicketCheckedDay == epochDay) return
        biliTicketCheckedDay = epochDay

        runCatching {
            val ts = (nowMs / 1000).toString()
            val hexsign = hmacSha256Hex(key = BILI_TICKET_HMAC_KEY, message = "ts$ts")
            val url = "https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket" +
                "?key_id=$BILI_TICKET_KEY_ID&hexsign=$hexsign&context[ts]=$ts"
            val request = okhttp3.Request.Builder()
                .url(url)
                .post(ByteArray(0).toRequestBody("application/json".toMediaType()))
                .build()
            val response = _okHttpClient.newCall(request).execute()
            val body = response.body?.string().orEmpty()
            val json = JSONObject(body)
            val data = json.optJSONObject("data") ?: return@runCatching
            val ticket = data.optString("ticket", "").trim()
            val createdAt = data.optLong("created_at", 0L)
            val ttl = data.optLong("ttl", 0L)
            if (ticket.isBlank() || createdAt <= 0L || ttl <= 0L) return@runCatching
            val expiresSec = createdAt + ttl
            val expiresAt = expiresSec * 1000L
            _cookieManager.saveCookies(
                listOf(
                    buildCookie("bili_ticket", ticket, expiresAt),
                    buildCookie("bili_ticket_expires", expiresSec.toString(), expiresAt)
                ).map { encodeCookieDirect(it) }
            )
            AppLog.d(TAG, "ensureBiliTicket success")
        }.onFailure {
            AppLog.w(TAG, "ensureBiliTicket failed: ${it.message}")
        }
    }

    private var buvidActivatedMid: Long = 0L
    private var buvidActivatedDay: Long = 0L

    private suspend fun ensureBuvidActiveOncePerDay() {
        val midStr = _cookieManager.getCookieValue("DedeUserID")?.trim().orEmpty()
        val mid = midStr.toLongOrNull()?.takeIf { it > 0 } ?: return
        val epochDay = System.currentTimeMillis() / 86_400_000L
        if (buvidActivatedMid == mid && buvidActivatedDay == epochDay) return

        runCatching {
            val rand = ByteArray(32 + 8 + 4)
            SecureRandom().nextBytes(rand)
            rand[32] = 0; rand[33] = 0; rand[34] = 0; rand[35] = 0
            rand[36] = 73; rand[37] = 69; rand[38] = 78; rand[39] = 68
            val tail = ByteArray(4)
            SecureRandom().nextBytes(tail)
            System.arraycopy(tail, 0, rand, 40, 4)
            val randPngEnd = android.util.Base64.encodeToString(rand, android.util.Base64.NO_WRAP)

            val jsonData = JSONObject()
                .put("3064", 1)
                .put("39c8", "333.1387.fp.risk")
                .put("3c43", JSONObject().put("adca", "Linux").put("bfe9", randPngEnd.takeLast(50)))
                .toString()

            val payload = JSONObject().put("payload", jsonData).toString()

            val cookieHeader = buildList {
                listOf("SESSDATA", "bili_jct", "DedeUserID", "DedeUserID__ckMd5", "sid", "buvid3").forEach { name ->
                    val v = _cookieManager.getCookieValue(name)?.takeIf { it.isNotBlank() } ?: return@forEach
                    add("$name=$v")
                }
            }.joinToString("; ")

            val request = okhttp3.Request.Builder()
                .url("https://api.bilibili.com/x/internal/gaia-gateway/ExClimbWuzhi")
                .post(payload.toRequestBody("application/json; charset=utf-8".toMediaType()))
                .header("Content-Type", "application/json")
                .header("env", "prod")
                .header("app-key", "android64")
                .header("x-bili-aurora-zone", "sh001")
                .header("x-bili-mid", mid.toString())
                .apply {
                    genAuroraEid(mid)?.let { header("x-bili-aurora-eid", it) }
                }
                .header("Referer", "https://www.bilibili.com")
                .header("Cookie", cookieHeader)
                .build()

            _okHttpClient.newCall(request).execute().use { resp ->
                if (resp.isSuccessful) {
                    buvidActivatedMid = mid
                    buvidActivatedDay = epochDay
                    AppLog.d(TAG, "ensureBuvidActiveOncePerDay ok mid=$mid")
                }
            }
        }.onFailure {
            AppLog.w(TAG, "ensureBuvidActiveOncePerDay failed: ${it.message}")
        }
    }

    private fun genAuroraEid(mid: Long): String? {
        if (mid <= 0) return null
        val key = "ad1va46a7lza".toByteArray()
        val input = mid.toString().toByteArray()
        val out = ByteArray(input.size)
        for (i in input.indices) out[i] = (input[i].toInt() xor key[i % key.size].toInt()).toByte()
        return Base64.encodeToString(out, Base64.NO_PADDING or Base64.NO_WRAP)
    }

    fun buildPiliWebHeaders(targetUrl: String, includeCookie: Boolean = true): Map<String, String> {
        val midStr = _cookieManager.getCookieValue("DedeUserID")?.trim().orEmpty()
        val mid = midStr.toLongOrNull()?.takeIf { it > 0 } ?: 0L
        val headers = mutableMapOf(
            "env" to "prod",
            "app-key" to "android64",
            "x-bili-aurora-zone" to "sh001",
            "Referer" to "https://www.bilibili.com",
            "X-Blbl-Skip-Origin" to "1"
        )
        if (mid > 0) {
            headers["x-bili-mid"] = mid.toString()
            genAuroraEid(mid)?.let { headers["x-bili-aurora-eid"] = it }
        }
        if (includeCookie) {
            val cookie = _cookieManager.getCookieHeaderFor(targetUrl)
            if (!cookie.isNullOrBlank()) headers["Cookie"] = cookie
        }
        return headers
    }

    private fun buildCookie(name: String, value: String, expiresAt: Long): Cookie {
        return Cookie.Builder()
            .name(name)
            .value(value)
            .domain("bilibili.com")
            .path("/")
            .expiresAt(expiresAt)
            .secure()
            .build()
    }

    private fun encodeCookieDirect(cookie: Cookie): String {
        val sb = StringBuilder()
        sb.append(cookie.name).append("=").append(cookie.value)
        sb.append("; domain=").append(cookie.domain.removePrefix("."))
        sb.append("; path=").append(cookie.path)
        if (cookie.secure) sb.append("; secure")
        if (cookie.expiresAt != Long.MAX_VALUE) sb.append("; expires=").append(cookie.expiresAt)
        return sb.toString()
    }

    private fun hmacSha256Hex(key: String, message: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(key.toByteArray(Charsets.UTF_8), "HmacSHA256"))
        val out = mac.doFinal(message.toByteArray(Charsets.UTF_8))
        val sb = StringBuilder(out.size * 2)
        for (b in out) sb.append(String.format("%02x", b))
        return sb.toString()
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
        prefs.edit {
            putString(KEY_CURRENT_UA, generated)
        }
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

    private var cookieRefreshCheckedDay: Long = 0L
    private val correspondPublicKey: PublicKey by lazy {
        val derBase64 =
            "MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQDLgd2OAkcGVtoE3ThUREbio0Eg" +
                "Uc/prcajMKXvkCKFCWhJYJcLkcM2DKKcSeFpD/j6Boy538YXnR6VhcuUJOhH2x71" +
                "nzPjfdTcqMz7djHum0qSZA0AyCBDABUqCrfNgCiJ00Ra7GmRj+YCK1NJEuewlb40" +
                "JNrRuoEUXpabUzGB8QIDAQAB"
        val keyBytes = Base64.decode(derBase64, Base64.DEFAULT)
        val spec = X509EncodedKeySpec(keyBytes)
        KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    private suspend fun refreshCookieIfNeededOncePerDay() {
        if (!_cookieManager.hasSessionCookie()) return
        val epochDay = System.currentTimeMillis() / 86_400_000L
        if (cookieRefreshCheckedDay == epochDay) return

        runCatching {
            val ts = System.currentTimeMillis()
            val correspondPath = getCorrespondPath(ts)
            val url = "https://www.bilibili.com/correspond/1/$correspondPath"
            val request = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", currentUA)
                .header("Referer", "https://www.bilibili.com")
                .build()
            val html = withContext(Dispatchers.IO) {
                _okHttpClient.newCall(request).execute().use { resp ->
                    resp.body?.string().orEmpty()
                }
            }
            val csrfMatch = Regex("<div\\s+id=\"1-name\">\\s*([0-9a-fA-F]{16,})\\s*</div>")
                .find(html)
            val refreshCsrf = csrfMatch?.groupValues?.get(1)?.trim().orEmpty()
            if (refreshCsrf.isBlank()) {
                AppLog.w(TAG, "refreshCookie: no csrf found in correspond page")
                cookieRefreshCheckedDay = epochDay
                return@runCatching
            }

            val sessdata = _cookieManager.getCookieValue("SESSDATA")?.trim().orEmpty()
            if (sessdata.isBlank()) return@runCatching

            val refreshUrl = "https://passport.bilibili.com/x/passport-login/web/cookie/refresh" +
                "?csrf=$refreshCsrf&refresh_csrf=$refreshCsrf&source=main_web&refresh_token="
            val refreshRequest = okhttp3.Request.Builder()
                .url(refreshUrl)
                .header("User-Agent", currentUA)
                .header("Referer", "https://www.bilibili.com")
                .header("Cookie", "SESSDATA=$sessdata")
                .post(ByteArray(0).toRequestBody("application/json".toMediaType()))
                .build()
            val refreshBody = withContext(Dispatchers.IO) {
                _okHttpClient.newCall(refreshRequest).execute().use { resp ->
                    resp.body?.string().orEmpty()
                }
            }
            val refreshJson = JSONObject(refreshBody)
            if (refreshJson.optInt("code", -1) != 0) {
                AppLog.w(TAG, "refreshCookie failed: code=${refreshJson.optInt("code")} msg=${refreshJson.optString("message")}")
                cookieRefreshCheckedDay = epochDay
                return@runCatching
            }
            val refreshData = refreshJson.optJSONObject("data") ?: JSONObject()
            val newSessdata = refreshData.optString("refresh_token", "").trim()
            val newTimestamp = refreshData.optLong("timestamp", 0L)

            val respCookies = mutableListOf<String>()
            if (newSessdata.isNotBlank()) {
                respCookies.add(
                    encodeCookieDirect(
                        buildCookie("SESSDATA", newSessdata, System.currentTimeMillis() + 180L * 24 * 60 * 60 * 1000)
                    )
                )
            }
            if (respCookies.isNotEmpty()) {
                _cookieManager.saveCookies(respCookies)
                AppLog.d(TAG, "refreshCookie success")
            }
            cookieRefreshCheckedDay = epochDay
        }.onFailure {
            AppLog.w(TAG, "refreshCookie failed: ${it.message}")
        }
    }

    private fun getCorrespondPath(timestampMs: Long): String {
        val plaintext = "refresh_${timestampMs}"
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        cipher.init(Cipher.ENCRYPT_MODE, correspondPublicKey, OAEPParameterSpec("SHA-256", "MGF1", MGF1ParameterSpec.SHA256, PSource.PSpecified.DEFAULT))
        val encrypted = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Base64.encodeToString(encrypted, Base64.NO_PADDING or Base64.NO_WRAP or Base64.URL_SAFE)
    }

    suspend fun postFormJson(url: String, form: Map<String, String>, extraHeaders: Map<String, String>? = null): JSONObject {
        return withContext(Dispatchers.IO) {
            val formBody = FormBody.Builder().apply {
                form.forEach { (k, v) -> add(k, v) }
            }.build()
            val reqBuilder = okhttp3.Request.Builder()
                .url(url)
                .post(formBody)
                .header("User-Agent", currentUA)
                .header("Referer", "https://www.bilibili.com")
                .header("Origin", "https://www.bilibili.com")
                .header("Content-Type", "application/x-www-form-urlencoded")
            extraHeaders?.forEach { (k, v) -> reqBuilder.header(k, v) }
            _okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                JSONObject(body)
            }
        }
    }

    suspend fun requestJson(url: String, extraHeaders: Map<String, String>? = null): JSONObject {
        return withContext(Dispatchers.IO) {
            val reqBuilder = okhttp3.Request.Builder()
                .url(url)
                .header("User-Agent", currentUA)
                .header("Referer", "https://www.bilibili.com")
            extraHeaders?.forEach { (k, v) -> reqBuilder.header(k, v) }
            _okHttpClient.newCall(reqBuilder.build()).execute().use { resp ->
                val body = resp.body?.string().orEmpty()
                JSONObject(body)
            }
        }
    }

    private fun isAuthInvalid(code: Int): Boolean {
        return code == AUTH_INVALID_CODE
    }
}

