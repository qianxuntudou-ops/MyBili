package com.tutu.myblbl.network.session

import android.content.SharedPreferences
import com.tutu.myblbl.model.BaseResponse
import com.tutu.myblbl.model.user.UserDetailInfoModel
import com.tutu.myblbl.network.WbiGenerator
import com.tutu.myblbl.network.response.Base2Response
import com.tutu.myblbl.network.response.BaseBaseResponse

class NetworkSessionStore(
    private val authInvalidCode: Int
) {

    companion object {
        private const val WBI_KEYS_STALE_MS = 24 * 60 * 60 * 1000L
        private const val PREFS_KEY_WBI_IMG = "wbi_img_key"
        private const val PREFS_KEY_WBI_SUB = "wbi_sub_key"
    }

    private var wbiImageKey: String = ""
    private var wbiSubKey: String = ""
    private var wbiKeysUpdatedAt: Long = 0L
    private var userInfo: UserDetailInfoModel? = null
    private var sharedPrefs: SharedPreferences? = null

    fun initPersistence(prefs: SharedPreferences) {
        sharedPrefs = prefs
        restorePersistedWbiKeys()
    }

    private fun restorePersistedWbiKeys() {
        val img = sharedPrefs?.getString(PREFS_KEY_WBI_IMG, "").orEmpty()
        val sub = sharedPrefs?.getString(PREFS_KEY_WBI_SUB, "").orEmpty()
        if (img.isNotBlank() && sub.isNotBlank()) {
            wbiImageKey = img
            wbiSubKey = sub
            wbiKeysUpdatedAt = System.currentTimeMillis()
        }
    }

    fun setWbiInfo(imgKey: String, subKey: String) {
        wbiImageKey = imgKey
        wbiSubKey = subKey
        if (imgKey.isNotBlank() && subKey.isNotBlank()) {
            wbiKeysUpdatedAt = System.currentTimeMillis()
        }
        sharedPrefs?.edit()?.apply {
            putString(PREFS_KEY_WBI_IMG, imgKey)
            putString(PREFS_KEY_WBI_SUB, subKey)
            apply()
        }
    }

    fun getWbiKeys(): Pair<String, String> {
        return Pair(wbiImageKey, wbiSubKey)
    }

    fun areWbiKeysStale(): Boolean {
        if (wbiImageKey.isBlank() || wbiSubKey.isBlank()) return true
        return System.currentTimeMillis() - wbiKeysUpdatedAt > WBI_KEYS_STALE_MS
    }

    fun getUserInfo(): UserDetailInfoModel? {
        return userInfo
    }

    fun clearUserSession() {
        userInfo = null
        wbiKeysUpdatedAt = 0L
        setWbiInfo("", "")
    }

    fun softClearUserSession() {
        userInfo = null
    }

    fun isSessionActive(): Boolean {
        return userInfo != null
    }

    fun updateUserSession(info: UserDetailInfoModel?) {
        userInfo = info?.let {
            it.copy(face = normalizeAvatarUrl(it.face))
        }
        if (userInfo == null) {
            setWbiInfo("", "")
            return
        }
        val normalizedUser = requireNotNull(userInfo)
        val imgKey = normalizedUser.wbiImg?.imgUrl?.let(WbiGenerator::extractKeyFromUrl).orEmpty()
        val subKey = normalizedUser.wbiImg?.subUrl?.let(WbiGenerator::extractKeyFromUrl).orEmpty()
        setWbiInfo(imgKey, subKey)
    }

    fun syncUserSession(
        response: BaseResponse<UserDetailInfoModel>,
        context: AuthContext = AuthContext.FOREGROUND,
        onAuthInvalid: (() -> Unit)? = null
    ): UserDetailInfoModel? {
        val info = response.data?.takeIf { response.isSuccess }
        if (info != null) {
            updateUserSession(info)
            return userInfo
        }
        if (isAuthInvalid(response.code)) {
            extractWbiKeysFromData(response.data)
            if (context.shouldClearSession) {
                val wasLoggedIn = userInfo != null
                softClearUserSession()
                if (wasLoggedIn) {
                    onAuthInvalid?.invoke()
                }
            }
        }
        return null
    }

    fun handleAuthFailureCode(
        code: Int,
        context: AuthContext = AuthContext.FOREGROUND,
        onAuthInvalid: (() -> Unit)? = null
    ) {
        if (isAuthInvalid(code)) {
            if (!context.shouldClearSession) return
            val wasLoggedIn = userInfo != null
            softClearUserSession()
            if (wasLoggedIn) {
                onAuthInvalid?.invoke()
            }
        }
    }

    fun <T> syncAuthState(
        response: BaseResponse<T>,
        context: AuthContext = AuthContext.FOREGROUND,
        onAuthInvalid: (() -> Unit)? = null
    ): BaseResponse<T> {
        handleAuthFailureCode(response.code, context, onAuthInvalid)
        return response
    }

    fun syncAuthState(
        response: BaseBaseResponse,
        context: AuthContext = AuthContext.FOREGROUND,
        onAuthInvalid: (() -> Unit)? = null
    ): BaseBaseResponse {
        handleAuthFailureCode(response.code, context, onAuthInvalid)
        return response
    }

    fun <T> syncAuthState(
        response: Base2Response<T>,
        context: AuthContext = AuthContext.FOREGROUND,
        onAuthInvalid: (() -> Unit)? = null
    ): Base2Response<T> {
        handleAuthFailureCode(response.code, context, onAuthInvalid)
        return response
    }

    private fun normalizeAvatarUrl(url: String): String = when {
        url.startsWith("http://") -> "https://${url.substring(7)}"
        url.startsWith("//") -> "https:$url"
        else -> url
    }

    private fun isAuthInvalid(code: Int): Boolean {
        return code == authInvalidCode
    }

    private fun extractWbiKeysFromData(data: UserDetailInfoModel?) {
        data?.wbiImg?.let { wbiImg ->
            val imgKey = WbiGenerator.extractKeyFromUrl(wbiImg.imgUrl)
            val subKey = WbiGenerator.extractKeyFromUrl(wbiImg.subUrl)
            if (imgKey.isNotBlank() && subKey.isNotBlank()) {
                setWbiInfo(imgKey, subKey)
            }
        }
    }
}
