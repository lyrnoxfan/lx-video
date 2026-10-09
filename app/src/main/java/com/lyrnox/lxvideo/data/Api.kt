package com.lyrnox.lxvideo.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

open class ApiException(message: String) : Exception(message)
class UnauthorizedException : ApiException("unauthorized")

/** 内存 Cookie 存储（只存 __Host-pdsession 会话） */
private class MemoryCookieJar : CookieJar {
    private val store = mutableMapOf<String, MutableList<Cookie>>()
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        store[url.host] = cookies.toMutableList()
    }
    override fun loadForRequest(url: HttpUrl): List<Cookie> {
        val now = System.currentTimeMillis()
        return (store[url.host] ?: emptyList()).filter { it.expiresAt > now }
    }
}

/** polydrive Worker API 客户端（视频 App 只需要登录 + 视频列表） */
class Api(baseUrl: String) {
    private val base = baseUrl.trimEnd('/')

    private val client: OkHttpClient = OkHttpClient.Builder()
        .cookieJar(MemoryCookieJar())
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .writeTimeout(180, TimeUnit.SECONDS)
        .build()

    private suspend fun call(method: String, path: String, json: JSONObject? = null): JSONObject =
        withContext(Dispatchers.IO) {
            val body = json?.toString()?.toRequestBody("application/json".toMediaType())
            val rb = Request.Builder().url(base + path)
            when (method) {
                "GET" -> rb.get()
                "POST" -> rb.post(body ?: ByteArray(0).toRequestBody())
            }
            client.newCall(rb.build()).execute().use { r ->
                if (r.code == 401) throw UnauthorizedException()
                val txt = r.body?.string().orEmpty()
                if (!r.isSuccessful) {
                    val msg = try {
                        JSONObject(txt).optString("error").ifBlank { "HTTP ${r.code}" }
                    } catch (_: Exception) {
                        "HTTP ${r.code}"
                    }
                    throw ApiException(msg)
                }
                if (txt.isBlank()) JSONObject() else JSONObject(txt)
            }
        }

    // ---------- 认证 ----------
    suspend fun login(password: String) {
        call("POST", "/api/login", JSONObject().put("password", password))
    }

    // ---------- 视频 feed ----------
    /** 短视频 feed：服务端按 mime/扩展名递归筛视频，url 为可直接播放的签名直链 */
    suspend fun getVideos(): List<VideoItem> {
        val j = call("GET", "/api/videos")
        val out = mutableListOf<VideoItem>()
        val arr = j.optJSONArray("videos")
        if (arr != null) for (i in 0 until arr.length()) out += arr.getJSONObject(i).toVideoItem()
        return out
    }
}
