package com.lughzdan.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.*
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.nio.ByteBuffer
import java.nio.charset.CodingErrorAction
import kotlin.coroutines.coroutineContext

data class SyncHttpRequest(val url:URL,val method:String,val bearer:String,val body:ByteArray?)
data class SyncHttpResponse(val status:Int,val body:ByteArray)
/** Injected only for local tests. The production implementation enforces HTTPS and response bounds. */
fun interface SyncHttpWire {suspend fun execute(request:SyncHttpRequest):SyncHttpResponse}
class UrlConnectionSyncWire:SyncHttpWire {
 override suspend fun execute(request:SyncHttpRequest):SyncHttpResponse=withContext(Dispatchers.IO) {
  require(request.url.protocol=="https" && request.url.userInfo==null)
  require(request.bearer.isNotBlank() && request.bearer.none{it=='\r'||it=='\n'})
  val connection=request.url.openConnection() as HttpURLConnection
  try {
   connection.instanceFollowRedirects=false;connection.connectTimeout=15000;connection.readTimeout=15000;connection.requestMethod=request.method
   connection.setRequestProperty("Authorization","Bearer ${request.bearer}");connection.setRequestProperty("Accept","application/json");connection.setRequestProperty("Accept-Encoding","identity")
   request.body?.let {bytes->require(bytes.size<=262144);connection.doOutput=true;connection.setRequestProperty("Content-Type","application/json");connection.setFixedLengthStreamingMode(bytes.size);connection.outputStream.use{it.write(bytes)}}
   coroutineContext.ensureActive();val status=connection.responseCode
   require(connection.contentEncoding.let{it==null || it=="identity"})
   require(connection.contentLengthLong<=262144)
   val output=ByteArrayOutputStream()
   (if(status in 200..299)connection.inputStream else connection.errorStream)?.use {input->val buffer=ByteArray(8192);while(true){coroutineContext.ensureActive();val n=input.read(buffer);if(n<0)break;require(output.size()+n<=262144){"Sync response limit"};output.write(buffer,0,n)}}
   SyncHttpResponse(status,output.toByteArray())
  }finally{connection.disconnect()}
 }
}
/** Endpoint origin is application configuration, never a redirect or a value from a sync response. */
class HttpSyncTransport(baseUrl:URL,private val wire:SyncHttpWire=UrlConnectionSyncWire()):SyncTransport {
 private val root=baseUrl.toExternalForm().trimEnd('/')
 init{require(baseUrl.protocol=="https" && baseUrl.userInfo==null && baseUrl.query==null && baseUrl.ref==null)}
 private suspend fun call(s:AuthSession,path:String,body:JsonObject?=null):JsonObject {
  val payload=body?.toString()?.toByteArray();require(payload==null || payload.size<=262144)
  val response=wire.execute(SyncHttpRequest(URL("$root$path"),if(body==null)"GET" else "POST",s.accessToken,payload))
  require(response.body.size<=262144)
  val text=Charsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).decode(ByteBuffer.wrap(response.body)).toString()
  if(response.status !in 200..299){val code=runCatching{Json.parseToJsonElement(text).jsonObject["error"]?.let{if(it is JsonPrimitive)it.content else it.jsonObject["code"]?.jsonPrimitive?.content}}.getOrNull()?:"HTTP_${response.status}";throw SyncHttpFailure(response.status,code.take(128))}
  return Json.parseToJsonElement(text).jsonObject
 }
 override suspend fun push(session:AuthSession,body:JsonObject):SyncPushReply {
  val reply=call(session,"/api/v1/sync/push",body)
  val results=reply.getValue("results").jsonArray;require(results.size<=100)
  return SyncPushReply(results.map{val r=it.jsonObject;SyncPushResult(r.getValue("operationId").jsonPrimitive.content,r.getValue("status").jsonPrimitive.content,(r["code"]?:r["error"])?.takeUnless{it is JsonNull}?.jsonPrimitive?.content,r["current"]?.takeUnless{it is JsonNull}?.jsonObject)},reply["cursor"]?.takeUnless{it is JsonNull}?.jsonPrimitive?.content)
 }
 override suspend fun pull(session:AuthSession,cursor:String?,limit:Int):SyncPullReply {
  require(limit in 1..100 && (cursor==null || cursor.length<=4096))
  val query="?limit=$limit"+(cursor?.let{"&cursor="+URLEncoder.encode(it,"UTF-8")}?:"")
  val reply=call(session,"/api/v1/sync/pull$query")
  val changes=reply.getValue("changes").jsonArray;require(changes.size<=limit)
  return SyncPullReply(changes.map{val c=it.jsonObject;SyncChange(c.getValue("version").jsonPrimitive.long,c.getValue("entityType").jsonPrimitive.content,c.getValue("entityId").jsonPrimitive.content,c.getValue("value").jsonObject)},reply.getValue("cursor").jsonPrimitive.content,reply.getValue("hasMore").jsonPrimitive.boolean)
 }
}
