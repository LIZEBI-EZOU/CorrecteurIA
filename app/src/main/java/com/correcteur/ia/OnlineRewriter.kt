package com.correcteur.ia

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OnlineRewriter {
    private val baseUrl: String
        get() = BuildConfig.CORRECTEURIA_API_BASE_URL.trimEnd('/')

    fun hasInternet(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun serverAvailable(): Boolean = try {
        val connection = (URL("$baseUrl/health").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 4000
            readTimeout = 5000
        }
        connection.responseCode in 200..299
    } catch (_: Throwable) {
        false
    }

    suspend fun rewrite(
        text: String,
        style: String,
        intensity: String
    ): String? = try {
        val connection = (URL("$baseUrl/v1/rewrite").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 7000
            readTimeout = 40000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        val body = JSONObject()
            .put("text", text)
            .put("style", style)
            .put("intensity", intensity)
            .toString()

        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        if (connection.responseCode !in 200..299) return null

        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        json.optString("output").takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }
}
