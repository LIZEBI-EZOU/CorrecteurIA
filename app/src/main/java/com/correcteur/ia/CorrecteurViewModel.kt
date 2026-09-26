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

data class CorrecteurState(val input:String="",val output:String="",val busy:Boolean=false,val message:String="Prêt — correction locale disponible.")
class CorrecteurViewModel(app: Application): AndroidViewModel(app) {
    private val _uiState=MutableStateFlow(CorrecteurState()); val uiState=_uiState.asStateFlow()
    private val ai=OnDeviceAi(app.applicationContext)
    fun setInput(value:String){_uiState.value=_uiState.value.copy(input=value,output="")}
    fun correct(){ val text=_uiState.value.input; if(text.isBlank())return; viewModelScope.launch {
        _uiState.value=_uiState.value.copy(busy=true,message="Correction locale LanguageTool…")
        val local=withContext(Dispatchers.Default){LanguageToolCorrector.correct(text)}
        val aiResult=if(text.length<=1200)ai.proofreadFrench(local) else null
        _uiState.value=_uiState.value.copy(output=aiResult?:local,busy=false,message=if(aiResult!=null)"LanguageTool + IA locale Gemini Nano." else "LanguageTool hors connexion — IA locale indisponible ou non compatible.")
    }}
    fun rewrite(){val text=_uiState.value.input;if(text.isBlank())return;viewModelScope.launch{
        _uiState.value=_uiState.value.copy(busy=true,message="Recherche de l’IA locale…")
        val aiResult=if(text.length<=1200)ai.rewriteFrench(text,RewriterOptions.OutputType.REPHRASE)else null
        val fallback=if(aiResult==null)withContext(Dispatchers.Default){OfflineFrenchCorrector.naturalRewrite(LanguageToolCorrector.correct(text))}else null
        _uiState.value=_uiState.value.copy(output=aiResult?:fallback.orEmpty(),busy=false,message=if(aiResult!=null)"Reformulation par IA sur l’appareil." else "IA locale indisponible : reformulation offline utilisée.")
    }}
    fun useResult(){val result=_uiState.value.output;_uiState.value=_uiState.value.copy(input=result,output="")}
    fun clearOutput(){_uiState.value=_uiState.value.copy(output="",message="Prêt.")}
    override fun onCleared(){ai.close();super.onCleared()}
}
