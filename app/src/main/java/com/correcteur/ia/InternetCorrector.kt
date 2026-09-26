package com.correcteur.ia

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object InternetCorrector {
    suspend fun correct(text: String): String? = try {
        val baseUrl = BuildConfig.CORRECTEURIA_API_BASE_URL.trimEnd("/")
        val connection = (URL("$baseUrl/v1/correct").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 7000
            readTimeout = 25000
            doOutput = true
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }

        val body = JSONObject().put("text", text).toString()
        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        if (connection.responseCode !in 200..299) return null

        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        json.optString("output").takeIf { it.isNotBlank() }
    } catch (_: Throwable) {
        null
    }
}
