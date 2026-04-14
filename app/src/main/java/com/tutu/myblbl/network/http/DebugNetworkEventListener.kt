package com.tutu.myblbl.network.http

import okhttp3.Call
import okhttp3.Connection
import okhttp3.EventListener
import okhttp3.Handshake
import okhttp3.HttpUrl
import okhttp3.Protocol
import okhttp3.Request
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Proxy
import java.util.concurrent.ConcurrentHashMap

class DebugNetworkEventListener : EventListener() {

    private data class TraceState(
        val callStartMs: Long = 0L,
        var dnsStartMs: Long = 0L,
        var dnsEndMs: Long = 0L,
        var connectStartMs: Long = 0L,
        var connectEndMs: Long = 0L,
        var secureConnectStartMs: Long = 0L,
        var secureConnectEndMs: Long = 0L,
        var requestHeadersStartMs: Long = 0L,
        var responseHeadersStartMs: Long = 0L,
        var responseHeadersEndMs: Long = 0L,
        var responseBodyEndMs: Long = 0L,
        var callEndMs: Long = 0L,
        var callFailed: Boolean = false,
        var errorMessage: String? = null
    )

    companion object {
        private const val TAG = "NetTrace"

        private val HOST_CATEGORIES = mapOf(
            "api.bilibili.com" to "api",
            "app.bilibili.com" to "api",
            "grpc.bilibili.com" to "api",
            "passport.bilibili.com" to "api",
            "api.vc.bilibili.com" to "api"
        )

        private fun categorizeHost(host: String): String {
            return HOST_CATEGORIES[host]
                ?: when {
                    host.contains("bilivideo") -> "media"
                    host.contains("akamaized") -> "media"
                    host.contains("mcdn") -> "media"
                    host.contains("vod") -> "media"
                    else -> "other"
                }
        }
    }

    private val traces = ConcurrentHashMap<Call, TraceState>()

    private fun urlOf(call: Call): HttpUrl = call.request().url

    private fun hostOf(call: Call): String = urlOf(call).host

    private fun pathOf(call: Call): String {
        val url = urlOf(call)
        return "${url.encodedPath}${url.encodedQuery?.let { "?$it" }.orEmpty()}"
    }

    override fun callStart(call: Call) {
        traces[call] = TraceState(callStartMs = System.currentTimeMillis())
    }

    override fun dnsStart(call: Call, domainName: String) {
        traces[call]?.let { it.dnsStartMs = System.currentTimeMillis() }
    }

    override fun dnsEnd(call: Call, domainName: String, inetAddressList: List<InetAddress>) {
        traces[call]?.let { it.dnsEndMs = System.currentTimeMillis() }
    }

    override fun connectStart(call: Call, address: InetSocketAddress, proxy: Proxy) {
        traces[call]?.let { it.connectStartMs = System.currentTimeMillis() }
    }

    override fun connectEnd(call: Call, address: InetSocketAddress, proxy: Proxy, protocol: Protocol?) {
        traces[call]?.let { it.connectEndMs = System.currentTimeMillis() }
    }

    override fun connectFailed(call: Call, address: InetSocketAddress, proxy: Proxy, protocol: Protocol?, ioe: java.io.IOException) {
        traces[call]?.let {
            it.connectEndMs = System.currentTimeMillis()
            it.callFailed = true
            it.errorMessage = ioe.message
        }
    }

    override fun secureConnectStart(call: Call) {
        traces[call]?.let { it.secureConnectStartMs = System.currentTimeMillis() }
    }

    override fun secureConnectEnd(call: Call, handshake: Handshake?) {
        traces[call]?.let { it.secureConnectEndMs = System.currentTimeMillis() }
    }

    override fun requestHeadersStart(call: Call) {
        traces[call]?.let { it.requestHeadersStartMs = System.currentTimeMillis() }
    }

    override fun responseHeadersStart(call: Call) {
        traces[call]?.let { it.responseHeadersStartMs = System.currentTimeMillis() }
    }

    override fun responseHeadersEnd(call: Call, response: okhttp3.Response) {
        traces[call]?.let { it.responseHeadersEndMs = System.currentTimeMillis() }
    }

    override fun responseBodyEnd(call: Call, byteCount: Long) {
        traces[call]?.let { it.responseBodyEndMs = System.currentTimeMillis() }
    }

    override fun callEnd(call: Call) {
        val trace = traces.remove(call) ?: return
        trace.callEndMs = System.currentTimeMillis()
        emitSummary(call, trace)
    }

    override fun callFailed(call: Call, ioe: java.io.IOException) {
        val trace = traces.remove(call) ?: return
        trace.callEndMs = System.currentTimeMillis()
        trace.callFailed = true
        trace.errorMessage = ioe.message
        emitSummary(call, trace)
    }

    private fun emitSummary(call: Call, trace: TraceState) {
        val host = hostOf(call)
        val path = pathOf(call).take(80)
        val category = categorizeHost(host)
        val totalMs = trace.callEndMs - trace.callStartMs

        val dnsMs = elapsed(trace.dnsStartMs, trace.dnsEndMs)
        val connectMs = elapsed(trace.connectStartMs, trace.connectEndMs)
        val tlsMs = elapsed(trace.secureConnectStartMs, trace.secureConnectEndMs)
        val ttfbMs = if (trace.responseHeadersStartMs > 0L && trace.requestHeadersStartMs > 0L) {
            trace.responseHeadersStartMs - trace.requestHeadersStartMs
        } else 0L
        val bodyMs = if (trace.responseBodyEndMs > 0L && trace.responseHeadersEndMs > 0L) {
            trace.responseBodyEndMs - trace.responseHeadersEndMs
        } else 0L

        if (trace.callFailed) {
            android.util.Log.w(TAG, buildString {
                append("[$category] FAIL $host$path")
                append(" | total=${totalMs}ms")
                append(" | error=${trace.errorMessage.orEmpty()}")
            })
        } else {
            android.util.Log.d(TAG, buildString {
                append("[$category] $host$path")
                append(" | total=${totalMs}ms")
                if (dnsMs > 0) append(" dns=${dnsMs}ms")
                if (connectMs > 0) append(" connect=${connectMs}ms")
                if (tlsMs > 0) append(" tls=${tlsMs}ms")
                if (ttfbMs > 0) append(" ttfb=${ttfbMs}ms")
                if (bodyMs > 0) append(" body=${bodyMs}ms")
            })
        }
    }

    private fun elapsed(startMs: Long, endMs: Long): Long {
        return if (startMs > 0L && endMs >= startMs) endMs - startMs else 0L
    }

    class Factory : EventListener.Factory {
        override fun create(call: Call): EventListener {
            return DebugNetworkEventListener()
        }
    }
}
