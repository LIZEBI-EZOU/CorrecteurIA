package com.correcteur.ia

import kotlinx.coroutines.delay
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object InternetCorrector {
    private const val MAX_ATTEMPTS = 3
    private const val CONNECT_TIMEOUT_MS = 10000
    private const val READ_TIMEOUT_MS = 60000

    suspend fun correct(text: String): String? {
        val baseUrl = BuildConfig.CORRECTEURIA_API_BASE_URL.trimEnd('/')

        repeat(MAX_ATTEMPTS) { attempt ->
            val result = try {
                val connection = (URL("$baseUrl/v1/correct").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = CONNECT_TIMEOUT_MS
                    readTimeout = READ_TIMEOUT_MS
                    doOutput = true
                    useCaches = false
                    setRequestProperty("Connection", "close")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Accept", "application/json")
                }

                try {
                    val body = JSONObject().put("text", text).toString()
                    connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
                    if (connection.responseCode !in 200..299) null
                    else {
                        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
                        json.optString("output").takeIf { it.isNotBlank() }
                    }
                } finally {
                    connection.disconnect()
                }
            } catch (_: Throwable) {
                null
            }

            if (result != null) return result
            if (attempt < MAX_ATTEMPTS - 1) delay(1000L * (attempt + 1))
        }

        return null
    }
}
