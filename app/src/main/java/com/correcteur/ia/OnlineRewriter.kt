package com.correcteur.ia

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OnlineRewriter {
    private const val MAX_ATTEMPTS = 3
    private const val HEALTH_CONNECT_TIMEOUT_MS = 10000
    private const val HEALTH_READ_TIMEOUT_MS = 15000
    private const val API_CONNECT_TIMEOUT_MS = 10000
    private const val API_READ_TIMEOUT_MS = 60000

    private val baseUrl: String
        get() = BuildConfig.CORRECTEURIA_API_BASE_URL.trimEnd('/')

    fun hasInternet(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    private suspend fun <T> withRetry(block: () -> T?): T? {
        var result: T? = null
        repeat(MAX_ATTEMPTS) { attempt ->
            result = try { block() } catch (_: Throwable) { null }
            if (result != null) return result
            if (attempt < MAX_ATTEMPTS - 1) delay(1000L * (attempt + 1))
        }
        return result
    }

    suspend fun serverAvailable(): Boolean = withRetry {
        (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = HEALTH_CONNECT_TIMEOUT_MS
            readTimeout = HEALTH_READ_TIMEOUT_MS
            useCaches = false
        }.let { connection ->
            try {
                connection.responseCode in 200..299
            } finally {
                connection.disconnect()
            }
        }
    } ?: false

    suspend fun rewrite(
        text: String,
        style: String,
        intensity: String
    ): String? = withRetry {
        val connection = (URL("$baseUrl/v1/rewrite").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = API_CONNECT_TIMEOUT_MS
            readTimeout = API_READ_TIMEOUT_MS
            doOutput = true
            useCaches = false
            setRequestProperty("Connection", "close")
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        try {
            val body = JSONObject()
                .put("text", text)
                .put("style", style)
                .put("intensity", intensity)
                .toString()

            connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (connection.responseCode !in 200..299) return@withRetry null

            val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
            json.optString("output").takeIf { it.isNotBlank() }
        } finally {
            connection.disconnect()
        }
    }
}
