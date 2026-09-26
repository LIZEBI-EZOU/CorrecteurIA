package com.correcteur.ia

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MemoryItem(val input: String, val output: String, val mode: String, val time: Long)

class MemoryStore(context: Context) {
    private val p = context.getSharedPreferences("correcteur_memory", Context.MODE_PRIVATE)
    fun isInternetEnabled() = p.getBoolean("internet_enabled", true)
    fun setInternetEnabled(value: Boolean) = p.edit().putBoolean("internet_enabled", value).apply()

    fun style() = p.getString("style", "Standard") ?: "Standard"
    fun setStyle(value: String) = p.edit().putString("style", value).apply()

    fun rewriteIntensity() = p.getString("rewrite_intensity", "Équilibré") ?: "Équilibré"
    fun setRewriteIntensity(value: String) = p.edit().putString("rewrite_intensity", value).apply()

    fun vocabulary(): List<String> = readStrings("vocabulary")
    fun protectedWords(): List<String> = readStrings("protected_words")

    fun learnedReplacements(): Map<String, String> {
        val array = JSONArray(p.getString("learned_replacements", "[]"))
        return buildMap {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                put(item.optString("from"), item.optString("to"))
            }
        }
    }

    fun addVocabulary(word: String) = addString("vocabulary", word)
    fun removeVocabulary(word: String) = removeString("vocabulary", word)
    fun addProtectedWord(word: String) = addString("protected_words", word)
    fun removeProtectedWord(word: String) = removeString("protected_words", word)

    fun learnReplacement(from: String, to: String) {
        if (from.isBlank() || to.isBlank()) return
        val old = learnedReplacements().toMutableMap()
        old[from.trim()] = to.trim()
        val array = JSONArray()
        old.entries.toList().takeLast(100).forEach { entry ->
            array.put(JSONObject().put("from", entry.key).put("to", entry.value))
        }
        p.edit().putString("learned_replacements", array.toString()).apply()
    }

    fun removeReplacement(from: String) {
        val old = learnedReplacements().toMutableMap()
        old.remove(from)
        val array = JSONArray()
        old.forEach { (fromValue, toValue) ->
            array.put(JSONObject().put("from", fromValue).put("to", toValue))
        }
        p.edit().putString("learned_replacements", array.toString()).apply()
    }

    fun personalize(text: String): String {
        var result = text
        learnedReplacements().entries.sortedByDescending { it.key.length }
            .forEach { (from, to) -> result = result.replace(from, to) }
        return result
    }

    fun protect(text: String): Pair<String, Map<String, String>> {
        var result = text
        val restore = linkedMapOf<String, String>()
        protectedWords().sortedByDescending { it.length }.forEachIndexed { index, word ->
            val token = "§MEMORY" + index + "§"
            if (result.contains(word)) {
                result = result.replace(word, token)
                restore[token] = word
            }
        }
        return result to restore
    }

    fun restore(text: String, map: Map<String, String>): String {
        var result = text
        map.forEach { (token, word) -> result = result.replace(token, word) }
        return result
    }

    fun save(input: String, output: String, mode: String) {
        if (input.isBlank() || output.isBlank()) return
        val old = JSONArray(p.getString("history", "[]"))
        val array = JSONArray()
        array.put(JSONObject().put("input", input).put("output", output).put("mode", mode).put("time", System.currentTimeMillis()))
        for (i in 0 until minOf(old.length(), 19)) array.put(old.getJSONObject(i))
        p.edit().putString("history", array.toString()).apply()
    }

    fun history(): List<MemoryItem> {
        val array = JSONArray(p.getString("history", "[]"))
        return buildList {
            for (i in 0 until array.length()) {
                val item = array.getJSONObject(i)
                add(MemoryItem(item.optString("input"), item.optString("output"), item.optString("mode"), item.optLong("time")))
            }
        }
    }

    fun clearHistory() = p.edit().remove("history").apply()

    fun clearPersonalMemory() = p.edit()
        .remove("vocabulary").remove("protected_words").remove("learned_replacements")
        .remove("style").remove("rewrite_intensity").apply()

    fun clear() = p.edit()
        .remove("history").remove("vocabulary").remove("protected_words")
        .remove("learned_replacements").remove("style").remove("rewrite_intensity").apply()

    private fun readStrings(key: String): List<String> {
        val array = JSONArray(p.getString(key, "[]"))
        return buildList { for (i in 0 until array.length()) add(array.optString(i)) }
    }

    private fun addString(key: String, value: String) {
        val clean = value.trim()
        if (clean.isBlank()) return
        val list = readStrings(key).toMutableList()
        if (list.none { it.equals(clean, ignoreCase = true) }) {
            list.add(clean)
            p.edit().putString(key, JSONArray(list.takeLast(100)).toString()).apply()
        }
    }

    private fun removeString(key: String, value: String) {
        val list = readStrings(key).filterNot { it.equals(value, ignoreCase = true) }
        p.edit().putString(key, JSONArray(list).toString()).apply()
    }
}
