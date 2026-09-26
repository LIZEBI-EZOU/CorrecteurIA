package com.correcteur.ia

data class CorrectionResult(val text: String, val changes: Int)

object OfflineFrenchCorrector {
    private val replacements = linkedMapOf(
        Regex("\\bje voudrai\\b", RegexOption.IGNORE_CASE) to "je voudrais",
        Regex("\\bje pourrai\\b", RegexOption.IGNORE_CASE) to "je pourrais",
        Regex("\\bsa va\\b", RegexOption.IGNORE_CASE) to "ça va",
        Regex("\\bca va\\b", RegexOption.IGNORE_CASE) to "ça va",
        Regex("\\bc'est\\s+pas\\b", RegexOption.IGNORE_CASE) to "ce n'est pas",
        Regex("\\by'a\\b", RegexOption.IGNORE_CASE) to "il y a",
        Regex("\\bparmis\\b", RegexOption.IGNORE_CASE) to "parmi",
        Regex("\\bmalgrès\\b", RegexOption.IGNORE_CASE) to "malgré",
        Regex("\\bquelque soit\\b", RegexOption.IGNORE_CASE) to "quel que soit",
        Regex("\\bau temps pour moi\\b", RegexOption.IGNORE_CASE) to "autant pour moi",
        Regex("\\bcomme meme\\b", RegexOption.IGNORE_CASE) to "quand même",
        Regex("\\bdesormais\\b", RegexOption.IGNORE_CASE) to "désormais",
        Regex("\\binterressant\\b", RegexOption.IGNORE_CASE) to "intéressant",
        Regex("\\bprofessionel\\b", RegexOption.IGNORE_CASE) to "professionnel",
        Regex("\\bconnection\\b", RegexOption.IGNORE_CASE) to "connexion",
        Regex("\\bdeveloppement\\b", RegexOption.IGNORE_CASE) to "développement",
        Regex("\\bprobleme\\b", RegexOption.IGNORE_CASE) to "problème",
        Regex("\\bnecessaire\\b", RegexOption.IGNORE_CASE) to "nécessaire",
        Regex("\\bfrancais\\b", RegexOption.IGNORE_CASE) to "français"
    )
    fun correct(source: String): CorrectionResult {
        var text = source
        var changes = 0
        for ((pattern, replacement) in replacements) {
            val before = text
            text = pattern.replace(text, replacement)
            if (text != before) changes++
        }
        val beforePunctuation = text
        text = text.replace(Regex("\\s+([,.!?;:])"), "$1")
            .replace(Regex("([.!?])([A-Za-zÀ-ÿ])"), "$1 $2")
            .replace(Regex("[ \\t]{2,}"), " ").trim()
        if (text != beforePunctuation) changes++
        if (text.isNotBlank()) {
            val fixed = text.replaceFirstChar { c -> if (c.isLowerCase()) c.titlecase() else c.toString() }
            if (fixed != text) { text = fixed; changes++ }
        }
        return CorrectionResult(text, changes)
    }
    fun naturalRewrite(source: String): String {
        val corrected = correct(source).text
        if (corrected.isBlank()) return corrected
        return corrected
            .replace(Regex("\\bJe voulais simplement vous dire que\\b", RegexOption.IGNORE_CASE), "Je souhaitais simplement vous préciser que")
            .replace(Regex("\\bJe veux\\b", RegexOption.IGNORE_CASE), "Je souhaite")
            .replace(Regex("\\bMerci beaucoup pour\\b", RegexOption.IGNORE_CASE), "Je vous remercie pour")
    }
}
