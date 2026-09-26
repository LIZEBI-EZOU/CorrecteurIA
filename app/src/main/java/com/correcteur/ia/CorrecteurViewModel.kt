package com.correcteur.ia

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.mlkit.genai.rewriting.RewriterOptions
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class CorrecteurState(
    val input: String = "",
    val output: String = "",
    val busy: Boolean = false,
    val message: String = "Prêt — correction locale disponible.",
    val internetEnabled: Boolean = true,
    val history: List<MemoryItem> = emptyList(),
    val style: String = "Standard",
    val rewriteIntensity: String = "Équilibré",
    val vocabulary: List<String> = emptyList(),
    val protectedWords: List<String> = emptyList(),
    val learnedReplacements: Map<String, String> = emptyMap(),
    val openAiConnected: Boolean = false,
    val onlineAvailable: Boolean = false
)

class CorrecteurViewModel(app: Application) : AndroidViewModel(app) {
    private val memory = MemoryStore(app.applicationContext)
    private val _uiState = MutableStateFlow(
        CorrecteurState(
            internetEnabled = memory.isInternetEnabled(),
            history = memory.history(),
            style = memory.style(),
            rewriteIntensity = memory.rewriteIntensity(),
            vocabulary = memory.vocabulary(),
            protectedWords = memory.protectedWords(),
            learnedReplacements = memory.learnedReplacements(),
            openAiConnected = memory.openAiApiKey().isNotBlank(),
            onlineAvailable = OnlineRewriter.hasInternet(app.applicationContext)
        )
    )
    val uiState = _uiState.asStateFlow()
    private val ai = OnDeviceAi(app.applicationContext)

    private fun refreshMemory(message: String? = null) {
        _uiState.value = _uiState.value.copy(
            style = memory.style(),
            rewriteIntensity = memory.rewriteIntensity(),
            vocabulary = memory.vocabulary(),
            protectedWords = memory.protectedWords(),
            learnedReplacements = memory.learnedReplacements(),
            message = message ?: _uiState.value.message
        )
    }

    fun setOpenAiApiKey(value: String) {
        memory.setOpenAiApiKey(value)
        _uiState.value = _uiState.value.copy(openAiConnected = value.isNotBlank(), message = if (value.isBlank()) "Accès en ligne retiré." else "Accès IA en ligne configuré.")
    }

    fun clearOpenAiApiKey() {
        memory.clearOpenAiApiKey()
        _uiState.value = _uiState.value.copy(openAiConnected = false, message = "Accès IA en ligne retiré.")
    }

    fun refreshConnectivity() {
        _uiState.value = _uiState.value.copy(onlineAvailable = OnlineRewriter.hasInternet(getApplication<Application>().applicationContext))
    }

    fun setInput(value: String) {
        _uiState.value = _uiState.value.copy(input = value, output = "")
    }

    fun setInternetEnabled(value: Boolean) {
        memory.setInternetEnabled(value)
        _uiState.value = _uiState.value.copy(
            internetEnabled = value,
            message = if (value) "Mode Internet activé." else "Mode privé hors connexion activé."
        )
    }

    fun setStyle(value: String) {
        memory.setStyle(value)
        refreshMemory("Style mémorisé : $value.")
    }

    fun setRewriteIntensity(value: String) {
        memory.setRewriteIntensity(value)
        refreshMemory("Intensité de reformulation : $value.")
    }

    fun addVocabulary(value: String) {
        memory.addVocabulary(value)
        refreshMemory("Mot ajouté au vocabulaire personnel.")
    }

    fun removeVocabulary(value: String) {
        memory.removeVocabulary(value)
        refreshMemory("Mot retiré du vocabulaire.")
    }

    fun addProtectedWord(value: String) {
        memory.addProtectedWord(value)
        refreshMemory("Mot protégé : il ne sera pas modifié.")
    }

    fun removeProtectedWord(value: String) {
        memory.removeProtectedWord(value)
        refreshMemory("Mot retiré des mots protégés.")
    }

    fun learnReplacement(from: String, to: String) {
        memory.learnReplacement(from, to)
        refreshMemory("Préférence mémorisée : « $from » → « $to ».")
    }

    fun removeReplacement(from: String) {
        memory.removeReplacement(from)
        refreshMemory("Préférence oubliée.")
    }

    fun correct() {
        val original = _uiState.value.input
        if (original.isBlank()) return

        viewModelScope.launch {
            val online = _uiState.value.internetEnabled
            _uiState.value = _uiState.value.copy(
                busy = true,
                message = if (online) "Correction hybride avec mémoire personnelle…" else "Correction locale avec mémoire personnelle…"
            )

            val (prepared, restore) = memory.protect(original)
            var result: String? = null
            var mode = "Hors connexion"

            if (online) {
                result = withContext(Dispatchers.IO) { InternetCorrector.correct(prepared) }
                if (result != null) mode = "Internet"
            }

            if (result == null) {
                val local = withContext(Dispatchers.Default) { LanguageToolCorrector.correct(prepared) }
                val aiResult = ai.proofreadFrench(local)
                result = aiResult ?: local
                if (aiResult != null) mode = "IA locale"
            }

            result = memory.restore(result.orEmpty(), restore)
            result = memory.personalize(result)
            memory.save(original, result, mode)

            _uiState.value = _uiState.value.copy(
                output = result,
                busy = false,
                message = "Correction terminée — $mode • mémoire personnalisée appliquée.",
                history = memory.history()
            )
        }
    }

    fun rewrite() {
        val original = _uiState.value.input
        if (original.isBlank()) return

        viewModelScope.launch {
            val state = _uiState.value
            val context = getApplication<Application>().applicationContext
            val online = OnlineRewriter.hasInternet(context)
            _uiState.value = state.copy(
                busy = true,
                onlineAvailable = online,
                message = if (online) "Reformulation en ligne — " + state.style + "…" else "Aucune connexion : reformulation locale…"
            )

            val (prepared, restore) = memory.protect(original)
            val key = memory.openAiApiKey()
            var result: String? = null
            var mode: String

            if (online) {
                if (key.isNotBlank()) {
                    result = withContext(Dispatchers.IO) {
                        OnlineRewriter.rewrite(prepared, state.style, state.rewriteIntensity, key)
                    }
                    mode = if (result != null) "IA en ligne" else "Service en ligne indisponible"
                } else {
                    mode = "Internet sans accès IA"
                }

                if (result == null) {
                    result = "Internet est disponible. Configurez l’accès IA dans ☰ > Connexion et IA en ligne pour reformuler. Le moteur local reste désactivé tant qu’Internet est disponible."
                }
            } else {
                val aiResult = ai.rewriteFrench(
                    prepared,
                    when (state.style) {
                        "Professionnel", "Formel" -> RewriterOptions.OutputType.PROFESSIONAL
                        "Simple", "Concis" -> RewriterOptions.OutputType.SHORTEN
                        "Chaleureux", "Amical" -> RewriterOptions.OutputType.FRIENDLY
                        "Détaillé" -> RewriterOptions.OutputType.ELABORATE
                        else -> RewriterOptions.OutputType.REPHRASE
                    }
                )
                result = aiResult ?: withContext(Dispatchers.Default) {
                    OfflineFrenchCorrector.naturalRewrite(LanguageToolCorrector.correct(prepared))
                }
                mode = if (aiResult != null) "IA locale hors ligne" else "Correcteur local hors ligne"
            }

            result = memory.restore(result.orEmpty(), restore)
            result = memory.personalize(result)
            memory.save(original, result, "Reformulation • " + state.style + " • " + state.rewriteIntensity + " • " + mode)

            _uiState.value = _uiState.value.copy(
                output = result,
                busy = false,
                onlineAvailable = online,
                openAiConnected = memory.openAiApiKey().isNotBlank(),
                message = if (online && key.isBlank()) "Internet détecté : moteur local désactivé. Configurez l’IA en ligne." else "Reformulation terminée — " + mode + ".",
                history = memory.history()
            )
        }
    }

    fun loadHistory(item: MemoryItem) {
        _uiState.value = _uiState.value.copy(input = item.input, output = item.output, message = "Souvenir chargé depuis la mémoire locale.")
    }

    fun clearHistory() {
        memory.clearHistory()
        _uiState.value = _uiState.value.copy(history = emptyList(), message = "Historique effacé.")
    }

    fun clearPersonalMemory() {
        memory.clearPersonalMemory()
        refreshMemory("Mémoire personnalisée effacée.")
    }

    fun clearMemory() {
        memory.clear()
        _uiState.value = _uiState.value.copy(
            history = emptyList(),
            vocabulary = emptyList(),
            protectedWords = emptyList(),
            learnedReplacements = emptyMap(),
            style = "Standard",
            rewriteIntensity = "Équilibré",
            message = "Toute la mémoire locale a été effacée."
        )
    }

    fun useResult() {
        _uiState.value = _uiState.value.copy(input = _uiState.value.output, output = "")
    }

    fun clearOutput() {
        _uiState.value = _uiState.value.copy(output = "", message = "Prêt.")
    }

    override fun onCleared() {
        ai.close()
        super.onCleared()
    }
}
