package com.correcteur.ia
import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
data class MemoryItem(val input:String,val output:String,val mode:String,val time:Long)
class MemoryStore(context:Context){
 private val p=context.getSharedPreferences("correcteur_memory",Context.MODE_PRIVATE)
 fun isInternetEnabled()=p.getBoolean("internet_enabled",true)
 fun setInternetEnabled(v:Boolean)=p.edit().putBoolean("internet_enabled",v).apply()
 fun save(input:String,output:String,mode:String){if(input.isBlank()||output.isBlank())return;val old=JSONArray(p.getString("history","[]"));val n=JSONArray();n.put(JSONObject().put("input",input).put("output",output).put("mode",mode).put("time",System.currentTimeMillis()));for(i in 0 until minOf(old.length(),19))n.put(old.getJSONObject(i));p.edit().putString("history",n.toString()).apply()}
 fun history():List<MemoryItem>{val a=JSONArray(p.getString("history","[]"));return buildList{for(i in 0 until a.length()){val o=a.getJSONObject(i);add(MemoryItem(o.optString("input"),o.optString("output"),o.optString("mode"),o.optLong("time")))}}}
 fun clear()=p.edit().remove("history").apply()
}