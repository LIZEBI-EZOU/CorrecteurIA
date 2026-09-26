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
data class CorrecteurState(val input:String="",val output:String="",val busy:Boolean=false,val message:String="Prêt — correction locale disponible.",val internetEnabled:Boolean=true,val history:List<MemoryItem> = emptyList())
class CorrecteurViewModel(app:Application):AndroidViewModel(app){
 private val memory=MemoryStore(app.applicationContext)
 private val _uiState=MutableStateFlow(CorrecteurState(internetEnabled=memory.isInternetEnabled(),history=memory.history()));val uiState=_uiState.asStateFlow()
 private val ai=OnDeviceAi(app.applicationContext)
 fun setInput(v:String){_uiState.value=_uiState.value.copy(input=v,output="")}
 fun setInternetEnabled(v:Boolean){memory.setInternetEnabled(v);_uiState.value=_uiState.value.copy(internetEnabled=v,message=if(v)"Mode Internet activé : le texte peut être envoyé au service en ligne." else "Mode privé hors connexion activé.")}
 fun correct(){val text=_uiState.value.input;if(text.isBlank())return;viewModelScope.launch{val online=_uiState.value.internetEnabled;_uiState.value=_uiState.value.copy(busy=true,message=if(online)"Correction Internet + IA locale…" else "Correction locale…");var result:String?=if(online)withContext(Dispatchers.IO){InternetCorrector.correct(text)}else null;var mode=if(result!=null)"Internet" else "Hors connexion";if(result==null){val local=withContext(Dispatchers.Default){LanguageToolCorrector.correct(text)};val aiResult=if(text.length<=1200)ai.proofreadFrench(local)else null;result=aiResult?:local;mode=if(aiResult!=null)"IA locale" else "Hors connexion"}memory.save(text,result!!,mode);_uiState.value=_uiState.value.copy(output=result,busy=false,message="Correction terminée — $mode.",history=memory.history())}}
 fun rewrite(){val text=_uiState.value.input;if(text.isBlank())return;viewModelScope.launch{_uiState.value=_uiState.value.copy(busy=true,message="Recherche de l’IA locale…");val r=if(text.length<=1200)ai.rewriteFrench(text,RewriterOptions.OutputType.REPHRASE)else null;val result=r?:withContext(Dispatchers.Default){OfflineFrenchCorrector.naturalRewrite(LanguageToolCorrector.correct(text))};val mode=if(r!=null)"IA locale" else "Hors connexion";memory.save(text,result,mode);_uiState.value=_uiState.value.copy(output=result,busy=false,message="Reformulation terminée — $mode.",history=memory.history())}}
 fun loadHistory(i:MemoryItem){_uiState.value=_uiState.value.copy(input=i.input,output=i.output,message="Souvenir chargé depuis la mémoire locale.")}
 fun clearMemory(){memory.clear();_uiState.value=_uiState.value.copy(history=emptyList(),message="Mémoire locale effacée.")}
 fun useResult(){val r=_uiState.value.output;_uiState.value=_uiState.value.copy(input=r,output="")}
 fun clearOutput(){_uiState.value=_uiState.value.copy(output="",message="Prêt.")}
 override fun onCleared(){ai.close();super.onCleared()}
}