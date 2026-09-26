package com.correcteur.ia
import android.content.Context
import com.google.mlkit.genai.common.FeatureStatus
import com.google.mlkit.genai.proofreading.*
import com.google.mlkit.genai.rewriting.*
import 
class OnDeviceAi(private val context: Context) {
    private var proofreader: Proofreader? = null
    private var rewriter: Rewriter? = null
    suspend fun proofreadFrench(text: String): String? {
        if (text.isBlank() || text.length > 1600) return null
        val options = ProofreaderOptions.builder(context).setInputType(ProofreaderOptions.InputType.KEYBOARD).setLanguage(ProofreaderOptions.Language.FRENCH).build()
        val client = Proofreading.getClient(options); proofreader = client
        return try {
            if (client.checkFeatureStatus().get() != FeatureStatus.AVAILABLE) null
            else client.runInference(ProofreadingRequest.builder(text).build()).get().results.firstOrNull()?.text
        } catch (_: Throwable) { null }
    }
    suspend fun rewriteFrench(text: String, style: Int = RewriterOptions.OutputType.REPHRASE): String? {
        if (text.isBlank() || text.length > 1600) return null
        val options = RewriterOptions.builder(context).setOutputType(style).setLanguage(RewriterOptions.Language.FRENCH).build()
        val client = Rewriting.getClient(options); rewriter = client
        return try {
            if (client.checkFeatureStatus().await() != FeatureStatus.AVAILABLE) null
            else client.runInference(RewritingRequest.builder(text).build()).get().results.firstOrNull()?.text
        } catch (_: Throwable) { null }
    }
    fun close() { proofreader?.close(); rewriter?.close(); proofreader = null; rewriter = null }
}
