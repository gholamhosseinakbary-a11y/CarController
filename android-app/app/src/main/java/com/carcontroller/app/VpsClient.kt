package com.carcontroller.app

// =====================================================================
// VpsClient.kt — کلاینت REST/WebSocket سرور (HTTPS با گواهی self-signed)
// ---------------------------------------------------------------------
// نکتهٔ امنیتی: گواهی سرور self-signed است؛ ترست‌آل با رمزنگاری TLS کامل
// برقرار می‌ماند (مثل «ادامه بده» مرورگر). مسیر API: docs/PROTOCOL.md
// =====================================================================

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

class VpsClient(val baseUrl: String, val token: String) {

    private val trustAll = object : X509TrustManager {
        override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
    }

    private fun sslCtx(): SSLContext {
        val c = SSLContext.getInstance("TLS")
        c.init(null, arrayOf(trustAll), SecureRandom())
        return c
    }

    private val http: OkHttpClient = OkHttpClient.Builder()
        .sslSocketFactory(sslCtx().socketFactory, trustAll)
        .hostnameVerifier { _, _ -> true }
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build()

    private val jsonMedia = "application/json; charset=utf-8".toMediaType()

    suspend fun request(method: String, path: String, body: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val url = baseUrl.trimEnd('/') + path
            val rb = Request.Builder().url(url).header("Authorization", "Bearer $token")
            val req = when {
                method == "GET" -> rb.get().build()
                method == "DELETE" -> rb.delete().build()
                body != null -> rb.method(method, body.toString().toRequestBody(jsonMedia)).build()
                else -> rb.method(method, "".toRequestBody(jsonMedia)).build()
            }
            http.newCall(req).execute().use { resp ->
                val text = resp.body?.string() ?: ""
                val obj = parseObj(text)
                    ?: JSONObject().put("success", false).put("message", "پاسخ نامعتبر سرور")
                if (!resp.isSuccessful) {
                    throw ApiException(resp.code, obj.str("message", obj.str("e")))
                }
                obj
            }
        }

    // ---------------- auth ----------------
    suspend fun login(phone: String, pass: String): JSONObject =
        request("POST", "/api/auth/login", JSONObject().put("phone", phone).put("password", pass))

    suspend fun register(phone: String, pass: String, name: String): JSONObject =
        request("POST", "/api/auth/register",
            JSONObject().put("phone", phone).put("password", pass).put("name", name))

    suspend fun logout(): JSONObject = request("POST", "/api/auth/logout")

    // ---------------- devices ----------------
    suspend fun devices(): List<JSONObject> =
        request("GET", "/api/devices").arr("devices")?.objs() ?: emptyList()

    suspend fun claimDevice(id: String, key: String, name: String): JSONObject =
        request("POST", "/api/devices",
            JSONObject().put("id", id).put("key", key).put("name", name))

    suspend fun removeDevice(id: String): JSONObject = request("DELETE", "/api/devices/$id")

    suspend fun patchDevice(id: String, name: String?, statInterval: Int?): JSONObject {
        val b = JSONObject()
        if (!name.isNullOrBlank()) b.put("name", name.trim())
        if (statInterval != null) b.put("stat_interval", statInterval)
        return request("PATCH", "/api/devices/$id", b)
    }

    suspend fun snapshot(id: String): JSONObject = request("GET", "/api/devices/$id/snapshot")

    // ---------------- commands ----------------
    suspend fun sendCmdRaw(id: String, c: String, a: JSONObject): JSONObject =
        request("POST", "/api/devices/$id/cmd", JSONObject().put("c", c).put("a", a))

    suspend fun typed(id: String, kind: String, action: String, body: JSONObject): JSONObject =
        request("POST", "/api/devices/$id/$kind/$action", body)

    suspend fun cmdStatus(id: String, cmdId: String): JSONObject =
        request("GET", "/api/devices/$id/queue/$cmdId")

    suspend fun cancelCmd(id: String, cmdId: String): JSONObject =
        request("DELETE", "/api/devices/$id/queue/$cmdId")

    // ---------------- history ----------------
    suspend fun events(id: String, since: Long = 0): List<JSONObject> =
        request("GET", "/api/devices/$id/events?since=$since").arr("events")?.objs() ?: emptyList()

    suspend fun sms(id: String): List<JSONObject> =
        request("GET", "/api/devices/$id/sms").arr("sms")?.objs() ?: emptyList()

    suspend fun locations(id: String, from: Long = 0): List<JSONObject> =
        request("GET", "/api/devices/$id/locations?from=$from").arr("locations")?.objs() ?: emptyList()

    suspend fun queue(id: String): JSONObject = request("GET", "/api/devices/$id/queue")

    // ---------------- share ----------------
    suspend fun share(id: String, phone: String): JSONObject =
        request("POST", "/api/devices/$id/share", JSONObject().put("phone", phone))

    suspend fun shares(id: String): List<JSONObject> =
        request("GET", "/api/devices/$id/share").arr("shares")?.objs() ?: emptyList()

    suspend fun unshare(id: String, userId: Int): JSONObject =
        request("DELETE", "/api/devices/$id/share/$userId")

    // ---------------- WebSocket زنده ----------------
    fun openWebSocket(onMsg: (String) -> Unit, onState: (Boolean) -> Unit): WebSocket {
        val wsBase = baseUrl.trimEnd('/')
            .replace("https://", "wss://")
            .replace("http://", "ws://")
        val url = wsBase + "/ws?token=" + java.net.URLEncoder.encode(token, "UTF-8")
        return http.newWebSocket(
            Request.Builder().url(url).build(),
            object : WebSocketListener() {
                override fun onOpen(webSocket: WebSocket, response: okhttp3.Response) { onState(true) }
                override fun onMessage(webSocket: WebSocket, text: String) { onMsg(text) }
                override fun onFailure(webSocket: WebSocket, t: Throwable, response: okhttp3.Response?) { onState(false) }
                override fun onClosed(webSocket: WebSocket, code: Int, reason: String) { onState(false) }
            }
        )
    }
}
