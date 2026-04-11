package com.tutu.myblbl.network.security

import com.tutu.myblbl.network.NetworkManager
import org.json.JSONObject

interface NetworkWebGateway {
    fun buildPiliWebHeaders(
        targetUrl: String,
        includeCookie: Boolean = true
    ): Map<String, String>

    suspend fun postFormJson(
        url: String,
        form: Map<String, String>,
        extraHeaders: Map<String, String>? = null
    ): JSONObject

    suspend fun requestJson(
        url: String,
        extraHeaders: Map<String, String>? = null
    ): JSONObject
}

class NetworkManagerWebGateway : NetworkWebGateway {
    override fun buildPiliWebHeaders(
        targetUrl: String,
        includeCookie: Boolean
    ): Map<String, String> {
        return NetworkManager.buildPiliWebHeaders(targetUrl, includeCookie)
    }

    override suspend fun postFormJson(
        url: String,
        form: Map<String, String>,
        extraHeaders: Map<String, String>?
    ): JSONObject {
        return NetworkManager.postFormJson(url, form, extraHeaders)
    }

    override suspend fun requestJson(
        url: String,
        extraHeaders: Map<String, String>?
    ): JSONObject {
        return NetworkManager.requestJson(url, extraHeaders)
    }
}
