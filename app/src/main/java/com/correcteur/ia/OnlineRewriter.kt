package com.correcteur.ia

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

object OnlineRewriter {
    fun hasInternet(context: Context): Boolean {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = manager.activeNetwork ?: return false
        val capabilities = manager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    suspend fun rewrite(
        text: String,
        style: String,
        intensity: String,
        apiKey: String
    ): String? = try {
        if (apiKey.isBlank()) return null

        val prompt = """
Tu es un excellent reformulateur français. Reformule le texte ci-dessous en conservant strictement son sens, ses faits, ses noms propres et ses informations.
Style demandé : $style.
Intensité : $intensity.
Le résultat doit être naturel, fluide, humain, élégant et directement utilisable.
Ne commente pas le texte. Ne parle pas d'IA. Retourne uniquement la reformulation.

TEXTE :
$text
""".trimIndent()

        val connection = (URL("https://api.openai.com/v1/responses").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 10000
            readTimeout = 30000
            doOutput = true
            setRequestProperty("Authorization", "Bearer $apiKey")
            setRequestProperty("Content-Type", "application/json")
        }

        val body = JSONObject()
            .put("model", "gpt-5.6-luna")
            .put("input", prompt)
            .put("max_output_tokens", 1600)
            .toString()

        connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }

        if (connection.responseCode !in 200..299) return null

        val json = JSONObject(connection.inputStream.bufferedReader().use { it.readText() })
        val output = json.optJSONArray("output") ?: return null

        for (i in 0 until output.length()) {
            val item = output.optJSONObject(i) ?: continue
            val content = item.optJSONArray("content") ?: continue
            for (j in 0 until content.length()) {
                val part = content.optJSONObject(j) ?: continue
                val value = part.optString("text")
                if (value.isNotBlank()) return value.trim()
            }
        }
        null
    } catch (_: Throwable) {
        null
    }
}
