package com.correcteur.ia

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

data class MemoryItem(val input:String,val output:String,val mode:String,val time:Long)

class MemoryStore(context:Context){
 private val p=context.getSharedPreferences("correcteur_memory",Context.MODE_PRIVATE)
 fun isInternetEnabled()=p.getBoolean("internet_enabled",true)
 fun setInternetEnabled(v:Boolean)=p.edit().putBoolean("internet_enabled",v).apply()
 fun style()=p.getString("style","Standard") ?: "Standard"
 fun setStyle(v:String)=p.edit().putString("style",v).apply()
 fun vocabulary():List<String> = readStrings("vocabulary")
 fun protectedWords():List<String> = readStrings("protected_words")
 fun learnedReplacements():Map<String,String>{
  val a=JSONArray(p.getString("learned_replacements","[]"))
  return buildMap { for(i in 0 until a.length()){val o=a.getJSONObject(i);put(o.optString("from"),o.optString("to"))} }
 }
 fun addVocabulary(word:String){addString("vocabulary",word)}
 fun removeVocabulary(word:String){removeString("vocabulary",word)}
 fun addProtectedWord(word:String){addString("protected_words",word)}
 fun removeProtectedWord(word:String){removeString("protected_words",word)}
 fun learnReplacement(from:String,to:String){
  if(from.isBlank()||to.isBlank())return
  val old=learnedReplacements().toMutableMap();old[from.trim()]=to.trim()
  val a=JSONArray();old.entries.takeLast(100).forEach{a.put(JSONObject().put("from",it.key).put("to",it.value))}
  p.edit().putString("learned_replacements",a.toString()).apply()
 }
 fun removeReplacement(from:String){
  val old=learnedReplacements().toMutableMap();old.remove(from)
  val a=JSONArray();old.forEach{(f,t)->a.put(JSONObject().put("from",f).put("to",t))}
  p.edit().putString("learned_replacements",a.toString()).apply()
 }
 fun personalize(text:String):String{
  var r=text
  learnedReplacements().entries.sortedByDescending{it.key.length}.forEach{(from,to)->r=r.replace(from,to)}
  return r
 }
 fun protect(text:String):Pair<String,Map<String,String>>{
  var r=text;val restore=linkedMapOf<String,String>()
  protectedWords().sortedByDescending{it.length}.forEachIndexed{i,w->
   val token="§MEMORY${i}§";if(r.contains(w)){r=r.replace(w,token);restore[token]=w}
  }
  return r to restore
 }
 fun restore(text:String,map:Map<String,String>):String{var r=text;map.forEach{(token,w)->r=r.replace(token,w)};return r}
 fun save(input:String,output:String,mode:String){
  if(input.isBlank()||output.isBlank())return
  val old=JSONArray(p.getString("history","[]"));val n=JSONArray()
  n.put(JSONObject().put("input",input).put("output",output).put("mode",mode).put("time",System.currentTimeMillis()))
  for(i in 0 until minOf(old.length(),19))n.put(old.getJSONObject(i))
  p.edit().putString("history",n.toString()).apply()
 }
 fun history():List<MemoryItem>{
  val a=JSONArray(p.getString("history","[]"))
  return buildList{for(i in 0 until a.length()){val o=a.getJSONObject(i);add(MemoryItem(o.optString("input"),o.optString("output"),o.optString("mode"),o.optLong("time")))}}
 }
 fun clearHistory()=p.edit().remove("history").apply()
 fun clearPersonalMemory()=p.edit().remove("vocabulary").remove("protected_words").remove("learned_replacements").remove("style").apply()
 fun clear()=p.edit().remove("history").remove("vocabulary").remove("protected_words").remove("learned_replacements").remove("style").apply()
 private fun readStrings(key:String):List<String>{val a=JSONArray(p.getString(key,"[]"));return buildList{for(i in 0 until a.length())add(a.optString(i))}}
 private fun addString(key:String,value:String){
  val v=value.trim();if(v.isBlank())return
  val list=readStrings(key).toMutableList();if(list.none{it.equals(v,true)}){list.add(v);p.edit().putString(key,JSONArray(list.takeLast(100)).toString()).apply()}
 }
 private fun removeString(key:String,value:String){val list=readStrings(key).filterNot{it.equals(value,true)};p.edit().putString(key,JSONArray(list).toString()).apply()}
}
