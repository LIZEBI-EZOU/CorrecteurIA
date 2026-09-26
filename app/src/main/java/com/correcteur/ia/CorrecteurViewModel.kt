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
 val input:String="",
 val output:String="",
 val busy:Boolean=false,
 val message:String="Prêt — correction locale disponible.",
 val internetEnabled:Boolean=true,
 val history:List<MemoryItem> = emptyList(),
 val style:String="Standard",
 val vocabulary:List<String> = emptyList(),
 val protectedWords:List<String> = emptyList(),
 val learnedReplacements:Map<String,String> = emptyMap()
)

class CorrecteurViewModel(app:Application):AndroidViewModel(app){
 private val memory=MemoryStore(app.applicationContext)
 private val _uiState=MutableStateFlow(CorrecteurState(
  internetEnabled=memory.isInternetEnabled(),history=memory.history(),style=memory.style(),
  vocabulary=memory.vocabulary(),protectedWords=memory.protectedWords(),learnedReplacements=memory.learnedReplacements()
 ))
 val uiState=_uiState.asStateFlow()
 private val ai=OnDeviceAi(app.applicationContext)

 private fun refreshMemory(message:String?=null){
  _uiState.value=_uiState.value.copy(
   style=memory.style(),vocabulary=memory.vocabulary(),protectedWords=memory.protectedWords(),
   learnedReplacements=memory.learnedReplacements(),message=message ?: _uiState.value.message)
 }
 fun setInput(v:String){_uiState.value=_uiState.value.copy(input=v,output="")}
 fun setInternetEnabled(v:Boolean){memory.setInternetEnabled(v);_uiState.value=_uiState.value.copy(internetEnabled=v,message=if(v)"Mode Internet activé." else "Mode privé hors connexion activé.")}
 fun setStyle(v:String){memory.setStyle(v);refreshMemory("Style mémorisé : $v.")}

 fun addVocabulary(v:String){memory.addVocabulary(v);refreshMemory("Mot ajouté au vocabulaire personnel.")}
 fun removeVocabulary(v:String){memory.removeVocabulary(v);refreshMemory("Mot retiré du vocabulaire.")}
 fun addProtectedWord(v:String){memory.addProtectedWord(v);refreshMemory("Mot protégé : il ne sera pas modifié.")}
 fun removeProtectedWord(v:String){memory.removeProtectedWord(v);refreshMemory("Mot retiré des mots protégés.")}
 fun learnReplacement(from:String,to:String){memory.learnReplacement(from,to);refreshMemory("Préférence mémorisée : « $from » → « $to ».")}
 fun removeReplacement(from:String){memory.removeReplacement(from);refreshMemory("Préférence oubliée.")}

 fun correct(){
  val original=_uiState.value.input
  if(original.isBlank())return
  viewModelScope.launch{
   val online=_uiState.value.internetEnabled
   _uiState.value=_uiState.value.copy(busy=true,message=if(online)"Correction hybride avec mémoire personnelle…" else "Correction locale avec mémoire personnelle…")
   val (prepared,restore)=memory.protect(original)
   var result:String?=null
   var mode="Hors connexion"
   if(online){result=withContext(Dispatchers.IO){InternetCorrector.correct(prepared)};if(result!=null)mode="Internet"}
   if(result==null){
    val local=withContext(Dispatchers.Default){LanguageToolCorrector.correct(prepared)}
    val aiResult:String?=if(prepared.length<=1200){ai.proofreadFrench(local)}else null
    result=aiResult ?: local
    if(aiResult!=null)mode="IA locale"
   }
   result=memory.restore(result ?: "",restore)
   result=memory.personalize(result)
   memory.save(original,result,mode)
   _uiState.value=_uiState.value.copy(output=result,busy=false,message="Correction terminée — $mode • mémoire personnalisée appliquée.",history=memory.history())
  }
 }

 fun rewrite(){
  val original=_uiState.value.input
  if(original.isBlank())return
  viewModelScope.launch{
   _uiState.value=_uiState.value.copy(busy=true,message="Reformulation avec votre mémoire personnelle…")
   val (prepared,restore)=memory.protect(original)
   val r:String?=if(prepared.length<=1200){ai.rewriteFrench(prepared,RewriterOptions.OutputType.REPHRASE)}else null
   var result=memory.restore(r ?: withContext(Dispatchers.Default){OfflineFrenchCorrector.naturalRewrite(LanguageToolCorrector.correct(prepared))},restore)
   result=memory.personalize(result)
   val mode=if(r!=null)"IA locale" else "Hors connexion"
   memory.save(original,result,mode)
   _uiState.value=_uiState.value.copy(output=result,busy=false,message="Reformulation terminée — $mode • préférences appliquées.",history=memory.history())
  }
 }
 fun loadHistory(i:MemoryItem){_uiState.value=_uiState.value.copy(input=i.input,output=i.output,message="Souvenir chargé depuis la mémoire locale.")}
 fun clearHistory(){memory.clearHistory();_uiState.value=_uiState.value.copy(history=emptyList(),message="Historique effacé.")}
 fun clearPersonalMemory(){memory.clearPersonalMemory();refreshMemory("Mémoire personnalisée effacée.")}
 fun clearMemory(){memory.clear();_uiState.value=_uiState.value.copy(history=emptyList(),vocabulary=emptyList(),protectedWords=emptyList(),learnedReplacements=emptyMap(),style="Standard",message="Toute la mémoire locale a été effacée.")}
 fun useResult(){val r=_uiState.value.output;_uiState.value=_uiState.value.copy(input=r,output="")}
 fun clearOutput(){_uiState.value=_uiState.value.copy(output="",message="Prêt.")}
 override fun onCleared(){ai.close();super.onCleared()}
}
