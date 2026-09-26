package com.correcteur.ia
import org.languagetool.JLanguageTool
import org.languagetool.language.French
object LanguageToolCorrector {
    fun correct(source: String): String {
        if (source.isBlank()) return source
        return try {
            val tool = JLanguageTool(French())
            var result = source
            tool.check(source).filter { it.suggestedReplacements.isNotEmpty() }
                .sortedByDescending { it.fromPos }.forEach { match ->
                    val replacement = match.suggestedReplacements.first()
                    result = result.substring(0, match.fromPos) + replacement + result.substring(match.toPos)
                }
            result
        } catch (_: Throwable) { OfflineFrenchCorrector.correct(source).text }
    }
}
