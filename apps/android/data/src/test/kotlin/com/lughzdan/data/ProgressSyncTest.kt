package com.lughzdan.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lughzdan.db.*
import com.lughzdan.engine.*
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.*
import org.junit.*
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Fake authenticated ports exercise persistence; they do not claim live trusted authentication. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class ProgressSyncTest {
 private lateinit var db:LughzdanDatabase
 private val puzzle="5637c78f-fe7e-528c-9763-9450426f7f18"
 private val device="5637c78f-fe7e-528c-9763-9450426f7f19"
 private val sessionId="5637c78f-fe7e-528c-9763-9450426f7f20"
 private val operationId="5637c78f-fe7e-528c-9763-9450426f7f21"
 private var identity:AuthSession?=null
 private var clock=1000L
 private val auth=AuthSessionProvider {identity}
 private class Transport:SyncTransport {
  val requests=mutableListOf<Pair<String,JsonObject>>()
  var push:(suspend (AuthSession,JsonObject)->SyncPushReply)?=null
  var page=SyncPullReply(emptyList(),"0",false)
  override suspend fun push(session:AuthSession,body:JsonObject):SyncPushReply {requests+=session.accountId to body;return push?.invoke(session,body) ?: SyncPushReply(body.getValue("operations").jsonArray.map{SyncPushResult(it.jsonObject.getValue("operationId").jsonPrimitive.content,"accepted")},"99")}
  override suspend fun pull(session:AuthSession,cursor:String?,limit:Int)=page
 }
 @Before fun setup()=runBlocking {
  db=Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext<Context>(),LughzdanDatabase::class.java).allowMainThreadQueries().build()
  val p=PuzzleDefinition(1,puzzle,1,"game","choice","words",1,1,"arabic_basic_v1","Test","published",false,listOf("test"),"Explanation",emptyList(),ChoicePayload("Q",listOf(Option("a","A"),Option("b","B")),"a"))
  db.games().insertPuzzles(listOf(LocalPuzzleEntity(puzzle,1,"test",SessionCodec.json.encodeToString(p),0)))
  db.games().enqueue(SyncOperationEntity(operationId,sessionId,"complete_session",SessionCodec.encode(GameSession(sessionId,puzzle,1,status=SessionStatus.WON,score=100))))
  Unit
 }
 @After fun close(){db.close()}
 private fun login(id:String="account-a"){identity=AuthSession(id,device,"test-token")}
 private fun sync(t:Transport)=ProgressSync(db,auth,t){clock}
 @Test fun workerSchedulingFailsClosedWithoutTrustedLoginOrApplicationOwner()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  assertFalse(SyncWorker.schedule(context,SignedOutSessionProvider))
  login();assertFalse(SyncWorker.schedule(context,auth))
 }
 @Test fun signedOutAndUnconfirmedAccountsNeverBindOrSend()=runBlocking {
  val t=Transport();val s=sync(t);assertEquals("SIGNED_OUT",s.syncOnce().state);login();assertEquals("MERGE_REQUIRED",s.syncOnce().state)
  assertEquals(1,db.content().unboundOperations("account-a").size);assertTrue(t.requests.isEmpty());assertEquals(1,db.games().operationCount())
 }
 @Test fun timeoutRetryUsesIdenticalWireIdsAndAcknowledgesWithoutDeletingReceipt()=runBlocking {
  login();val t=Transport();t.push={_,_->throw java.io.IOException("lost response")};var s=sync(t);s.confirmMerge();assertEquals("PUSH_FAILED",s.syncOnce().state)
  val pending=db.content().pending("account-a",Long.MAX_VALUE).single();assertEquals(1,pending.retries);assertTrue(pending.nextRetryAt>clock)
  assertEquals("SYNCED",s.syncOnce().state);assertEquals(1,t.requests.size)
  clock=pending.nextRetryAt;t.push=null;s=sync(t);assertEquals(1,s.syncOnce().pushed)
  assertEquals(t.requests[0].second,t.requests[1].second);assertEquals(1,db.games().operationCount());assertTrue(db.content().pending("account-a",Long.MAX_VALUE).isEmpty())
  val op=t.requests.last().second.getValue("operations").jsonArray.single().jsonObject
  assertEquals(setOf("operationId","deviceId","deviceSeq","kind","payload"),op.keys)
  assertEquals(setOf("sessionId","puzzleId","puzzleRevision","rulesVersion","outcome","score","wrongSubmissions","hintIds","durationMs"),op.getValue("payload").jsonObject.keys)
  assertEquals("0",db.content().account("account-a")!!.cursor) // never adopts push cursor 99
 }
 @Test fun authChangeInFlightCannotApplyReplyOrCrossSendAccountQueue()=runBlocking {
  login();val t=Transport();t.push={_,_->login("account-b");SyncPushReply(listOf(SyncPushResult(operationId,"accepted")))};val s=sync(t);s.confirmMerge()
  assertEquals("ACCOUNT_CHANGED",s.syncOnce().state);assertEquals(1,db.content().pending("account-a",Long.MAX_VALUE).size)
  assertEquals("MERGE_REQUIRED",s.syncOnce().state);assertEquals(1,t.requests.size)
  s.confirmMerge();t.push=null;s.syncOnce();assertEquals("account-b",t.requests.last().first);assertEquals(1,db.content().pending("account-a",Long.MAX_VALUE).size)
 }
 private fun progress(version:Long,score:Int,completed:Boolean)=SyncChange(version,"progress",puzzle,buildJsonObject {put("puzzleId",puzzle);put("serverVersion",version);put("bestScore",score);put("attempts",1);put("completed",completed)})
 @Test fun pullMergesMonotonicallyAndCursorRollsBackWithInvalidPage()=runBlocking {
  login();db.games().saveProgress(PlayerProgressEntity(puzzle,200,true,3,"choice",200));val t=Transport();val s=sync(t);s.confirmMerge()
  t.page=SyncPullReply(listOf(progress(1,50,false),SyncChange(2,"achievement","remote_win",buildJsonObject {put("achievementId","remote_win");put("serverVersion",2)})),"2",false)
  s.syncOnce();assertEquals(200,db.games().progress(puzzle)!!.bestScore);assertTrue(db.games().progress(puzzle)!!.completed);assertEquals(3,db.games().progress(puzzle)!!.attempts);assertEquals("2",db.content().account("account-a")!!.cursor)
  t.page=SyncPullReply(listOf(progress(3,250,true),progress(4,500,true)),"4",false)
  try{s.syncOnce();fail("Invalid page accepted")}catch(_:IllegalArgumentException){}
  assertEquals(200,db.games().progress(puzzle)!!.bestScore);assertEquals("2",db.content().account("account-a")!!.cursor);assertNull(db.content().replica("account-a","progress",puzzle)?.takeIf{it.serverVersion==3L})
  assertEquals(1,db.games().operationCount())
 }
 @Test fun permanentRejectionAndConflictAreNotRetried()=runBlocking {
  for(status in listOf("rejected","conflict")) {
   login("account-$status");val t=Transport();t.push={_,_->SyncPushReply(listOf(SyncPushResult(operationId,status,"PERMANENT")))};val s=sync(t);s.confirmMerge();s.syncOnce();s.syncOnce()
   assertEquals(1,t.requests.size);assertEquals(1,db.games().operationCount());assertEquals(0,db.content().pending("account-$status",Long.MAX_VALUE).size)
  }
 }
 @Test fun draftAndFixtureReceiptsNeverUpload()=runBlocking {
  val row=db.games().puzzle(puzzle,1)!!
  val original=SessionCodec.json.decodeFromString<PuzzleDefinition>(row.definition)
  db.games().removeContent("test")
  db.games().insertPuzzles(listOf(row.copy(definition=SessionCodec.json.encodeToString(original.copy(status="draft",fixtureOnly=true)))))
  login();val t=Transport();val s=sync(t);s.confirmMerge();s.syncOnce()
  assertTrue(t.requests.isEmpty());assertTrue(db.content().unboundOperations("account-a").isEmpty());assertEquals(1,db.games().operationCount())
 }
 @Test fun invalidLocalReceiptIsDeadletteredWithoutNetworkPush()=runBlocking {
  db.games().enqueue(SyncOperationEntity("not-uuid","other-session","complete_session","{}"));login();val t=Transport();val s=sync(t);s.confirmMerge();s.syncOnce()
  assertEquals(1,t.requests.single().second.getValue("operations").jsonArray.size);assertEquals(2,db.games().operationCount());assertTrue(db.content().unboundOperations("account-a").isEmpty())
 }
}
