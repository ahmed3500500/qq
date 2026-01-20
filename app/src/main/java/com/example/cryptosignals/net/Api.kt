package com.example.cryptosignals.net

import kotlinx.serialization.json.Json
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

class Api(private val baseUrl: String) {
    private val client = OkHttpClient()
    private val json = Json { ignoreUnknownKeys = true }

    private fun url(path: String, query: Map<String, String> = emptyMap()): String {
        val u = baseUrl.trimEnd('/').toHttpUrl().newBuilder().addPathSegments(path.trimStart('/'))
        for ((k,v) in query) u.addQueryParameter(k, v)
        return u.build().toString()
    }

    fun health(): Result<HealthResponse> = runCatching {
        val req = Request.Builder().url(url("/health")).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${'$'}{resp.code}")
            json.decodeFromString(HealthResponse.serializer(), resp.body!!.string())
        }
    }

    fun latest(limit: Int): Result<SignalsResponse> = runCatching {
        val req = Request.Builder().url(url("/signals/latest", mapOf("limit" to limit.toString()))).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${'$'}{resp.code}")
            json.decodeFromString(SignalsResponse.serializer(), resp.body!!.string())
        }
    }

    fun opportunities(limit: Int): Result<SignalsResponse> = runCatching {
        val req = Request.Builder().url(url("/signals/opportunities", mapOf("limit" to limit.toString()))).get().build()
        client.newCall(req).execute().use { resp ->
            if (!resp.isSuccessful) error("HTTP ${'$'}{resp.code}")
            json.decodeFromString(SignalsResponse.serializer(), resp.body!!.string())
        }
    }
}
