package com.lughzdan.data

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Test
import java.net.URL

class HttpSyncTransportTest {
 private val session=AuthSession("test-account","5637c78f-fe7e-528c-9763-9450426f7f19","trusted-test-token")
 @Test fun mapsAuthenticatedPushAndPullWithoutAddingAccountToBody()=runBlocking {
  val requests=mutableListOf<SyncHttpRequest>()
  val transport=HttpSyncTransport(URL("https://api.example.test"),SyncHttpWire {request->requests+=request;SyncHttpResponse(200,if(request.method=="POST")"""{"results":[{"operationId":"op","status":"duplicate","serverVersion":1}],"cursor":"1"}""".toByteArray() else """{"changes":[{"version":1,"ordinal":0,"entityType":"progress","entityId":"p","value":{"serverVersion":1}}],"cursor":"opaque+/=","hasMore":false}""".toByteArray())})
  val body=buildJsonObject{put("operations",JsonArray(emptyList()))}
  assertEquals("duplicate",transport.push(session,body).results.single().status)
  assertEquals("opaque+/=",transport.pull(session,"opaque+/=",100).cursor)
  assertEquals("https://api.example.test/api/v1/sync/push",requests[0].url.toExternalForm());assertEquals(session.accessToken,requests[0].bearer)
  assertEquals(body.toString(),requests[0].body!!.toString(Charsets.UTF_8));assertEquals("https://api.example.test/api/v1/sync/pull?limit=100&cursor=opaque%2B%2F%3D",requests[1].url.toExternalForm())
 }
 @Test fun rejectsInsecureOriginOversizedReplyAndHttpErrors()=runBlocking {
  try{HttpSyncTransport(URL("http://api.example.test"));fail()}catch(_:IllegalArgumentException){}
  val oversized=HttpSyncTransport(URL("https://api.example.test"),SyncHttpWire {SyncHttpResponse(200,ByteArray(262145))})
  try{oversized.pull(session,null,100);fail()}catch(_:IllegalArgumentException){}
  val expired=HttpSyncTransport(URL("https://api.example.test"),SyncHttpWire {SyncHttpResponse(410,"""{"error":{"code":"SNAPSHOT_REQUIRED"}}""".toByteArray())})
  try{expired.pull(session,null,100);fail()}catch(e:SyncHttpFailure){assertEquals(410,e.status);assertEquals("SNAPSHOT_REQUIRED",e.code)}
 }
}
