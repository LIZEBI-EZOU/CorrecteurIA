package com.correcteur.ia
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URLEncoder
import java.net.URL
object InternetCorrector {
 suspend fun correct(text:String):String?=try{
  val c=(URL("https://api.languagetool.org/v2/check").openConnection() as HttpURLConnection).apply{requestMethod="POST";connectTimeout=8000;readTimeout=12000;doOutput=true;setRequestProperty("Content-Type","application/x-www-form-urlencoded")}
  c.outputStream.use{it.write(("text="+URLEncoder.encode(text,"UTF-8")+"&language=fr").toByteArray())}
  if(c.responseCode !in 200..299)return null
  val a=JSONObject(c.inputStream.bufferedReader().use{it.readText()}).optJSONArray("matches")?:return text
  var r=text
  for(i in a.length()-1 downTo 0){val m=a.getJSONObject(i);val q=m.optJSONArray("replacements");if(q!=null&&q.length()>0){val v=q.getJSONObject(0).optString("value");val o=m.getInt("offset");val l=m.getInt("length");if(v.isNotEmpty()&&o+l<=r.length)r=r.substring(0,o)+v+r.substring(o+l)}}
  r
 }catch(_:Exception){null}
}