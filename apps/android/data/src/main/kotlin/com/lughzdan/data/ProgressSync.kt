package com.lughzdan.data

import androidx.room.withTransaction
import com.lughzdan.db.*
import com.lughzdan.engine.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.*
import java.util.UUID

/** Supplied only by a trusted login/device-registration implementation; no identity from user input. */
data class AuthSession(val accountId:String,val deviceId:String,val accessToken:String)
fun interface AuthSessionProvider { suspend fun current():AuthSession? }
object SignedOutSessionProvider:AuthSessionProvider {override suspend fun current():AuthSession?=null}
data class SyncPushResult(val operationId:String,val status:String,val error:String?=null,val current:JsonObject?=null)
data class SyncPushReply(val results:List<SyncPushResult>,val cursor:String?=null)
data class SyncChange(val version:Long,val entityType:String,val entityId:String,val value:JsonObject)
data class SyncPullReply(val changes:List<SyncChange>,val cursor:String,val hasMore:Boolean)
interface SyncTransport {
 suspend fun push(session:AuthSession,body:JsonObject):SyncPushReply
 suspend fun pull(session:AuthSession,cursor:String?,limit:Int):SyncPullReply
}
/** HTTP adapters must distinguish permanent request failures, auth changes and retryable outages. */
class SyncHttpFailure(val status:Int,val code:String):RuntimeException(code)
data class SyncRunResult(val state:String,val pushed:Int=0,val pulled:Int=0,val hasMore:Boolean=false)

/** Local receipts survive delivery. Frozen wire JSON, sequence and ownership are committed together. */
class ProgressSync(private val db:LughzdanDatabase,private val auth:AuthSessionProvider=SignedOutSessionProvider,
 private val transport:SyncTransport,private val now:()->Long=System::currentTimeMillis) {
 private val mutex=Mutex()
 private val dao=db.content()
 private suspend fun session():AuthSession?=auth.current()?.also {require(it.accountId.isNotBlank() && it.accessToken.isNotBlank());uuid(it.deviceId)}
 private suspend fun unchanged(s:AuthSession):Boolean=auth.current()?.let{it.accountId==s.accountId && it.deviceId==s.deviceId}==true
 suspend fun confirmMerge()=mutex.withLock {
  val s=session() ?: error("Authentication required")
  db.withTransaction {val old=dao.account(s.accountId);require(old==null || old.deviceId==s.deviceId){"Device registration changed"};dao.account(old?.copy(mergeConfirmed=true) ?: SyncAccountEntity(s.accountId,s.deviceId,1,null,true))}
 }
 suspend fun syncOnce():SyncRunResult=mutex.withLock {
  val s=session() ?: return@withLock SyncRunResult("SIGNED_OUT")
  val account=dao.account(s.accountId)
  if(account?.mergeConfirmed!=true)return@withLock SyncRunResult("MERGE_REQUIRED")
  require(account.deviceId==s.deviceId){"Device registration changed"}
  bind(s)
  if(!unchanged(s))return@withLock SyncRunResult("ACCOUNT_CHANGED")
  val pending=dao.pending(s.accountId,now(),100)
  val batch=mutableListOf<SyncDeliveryEntity>()
  for(row in pending){val candidate=batch+row;if(body(candidate).toString().toByteArray().size>256*1024)break;batch+=row}
  var pushed=0
  if(batch.isNotEmpty()) {
   try {
    val reply=transport.push(s,body(batch))
    if(!unchanged(s))return@withLock SyncRunResult("ACCOUNT_CHANGED")
    require(reply.results.map{it.operationId}.distinct().size==reply.results.size && reply.results.all{r->batch.any{it.operationId==r.operationId}}){"Invalid push receipt"}
    db.withTransaction {for(row in batch){val result=reply.results.firstOrNull{it.operationId==row.operationId};when(result?.status){
     "accepted","applied","duplicate"->{dao.delivery(row.copy(status="ACK",error=null));pushed++}
     "rejected"->dao.delivery(row.copy(status="DEADLETTER",error=result.error?:"REJECTED"))
     "conflict"->dao.delivery(row.copy(status="CONFLICT",error=buildJsonObject {put("code",result.error?:"CONFLICT");put("current",result.current?:JsonNull)}.toString()))
     else->retry(row,"MISSING_OR_TRANSIENT_RESULT")
    }}}
    // A push cursor is never a pull checkpoint: another device may have unseen changes before it.
   }catch(e:CancellationException){throw e}catch(e:Exception){
    if(!unchanged(s))return@withLock SyncRunResult("ACCOUNT_CHANGED")
    db.withTransaction {batch.forEach {row->when{e is SyncHttpFailure && e.status==409->dao.delivery(row.copy(status="CONFLICT",error=e.code));e is SyncHttpFailure && e.status in listOf(400,404,422)->dao.delivery(row.copy(status="DEADLETTER",error=e.code));else->retry(row,if(e is SyncHttpFailure)e.code else "TRANSPORT")}}}
    return@withLock SyncRunResult(if(e is SyncHttpFailure && e.status in listOf(401,403))"AUTH_REQUIRED" else "PUSH_FAILED")
   }
  }
  if(!unchanged(s))return@withLock SyncRunResult("ACCOUNT_CHANGED",pushed)
  val cursor=dao.account(s.accountId)!!.cursor
  val page=try{transport.pull(s,cursor,100)}catch(e:CancellationException){throw e}catch(e:SyncHttpFailure){return@withLock SyncRunResult(if(e.status==410)"SNAPSHOT_REQUIRED" else "PULL_FAILED",pushed)}catch(e:Exception){return@withLock SyncRunResult("PULL_FAILED",pushed)}
  if(!unchanged(s))return@withLock SyncRunResult("ACCOUNT_CHANGED",pushed)
  require(page.changes.size<=100 && page.cursor.length<=4096 && (!page.hasMore || page.cursor!=cursor))
  db.withTransaction {
   var previous=-1L
   page.changes.forEach {change->require(change.version>=previous && change.version in 0..9007199254740991L);previous=change.version;merge(s.accountId,change)}
   dao.account(dao.account(s.accountId)!!.copy(cursor=page.cursor))
  }
  SyncRunResult("SYNCED",pushed,page.changes.size,page.hasMore)
 }
 private suspend fun bind(s:AuthSession)=db.withTransaction {
  var account=dao.account(s.accountId)!!
  for(receipt in dao.unboundOperations(s.accountId,100)) {
   require(account.nextSeq in 1..2147483647L){"Device sequence exhausted; register a new device"}
   val wire=try {Result.success(operation(receipt,account))}catch(e:CancellationException){throw e}catch(e:Exception){Result.failure<JsonObject>(e)}
   val payload=wire.getOrNull()?.toString()?:"{}"
   val valid=wire.isSuccess && payload.toByteArray().size<256*1024-32
   dao.delivery(SyncDeliveryEntity(s.accountId,receipt.operationId,s.deviceId,account.nextSeq,payload,if(valid)"PENDING" else "DEADLETTER",0,0,if(valid)null else "INVALID_LOCAL_RECEIPT"))
   account=account.copy(nextSeq=account.nextSeq+1)
  }
  dao.account(account)
 }
 private suspend fun operation(receipt:SyncOperationEntity,a:SyncAccountEntity):JsonObject {
  require(receipt.kind=="complete_session");uuid(receipt.operationId)
  val s=SessionCodec.decode(receipt.payload);uuid(s.id);uuid(s.puzzleId);require(s.id==receipt.sessionId && s.status in setOf(SessionStatus.WON,SessionStatus.LOST))
  require(s.revision>0 && s.score in 0..300 && s.wrongSubmissions>=0 && s.usedHintIds.size<=10 && s.usedHintIds.all{it.length in 1..2000})
  val row=db.games().puzzle(s.puzzleId,s.revision) ?: error("Pinned definition missing")
  val p=SessionCodec.json.decodeFromString<PuzzleDefinition>(row.definition);require(p.rulesVersion>0 && p.status=="published" && !p.fixtureOnly) {"Only published non-fixture progress can sync"}
  return buildJsonObject {put("operationId",receipt.operationId);put("deviceId",a.deviceId);put("deviceSeq",a.nextSeq);put("kind","complete_session");put("payload",buildJsonObject {
   put("sessionId",s.id);put("puzzleId",s.puzzleId);put("puzzleRevision",s.revision);put("rulesVersion",p.rulesVersion);put("outcome",if(s.status==SessionStatus.WON)"won" else "lost");put("score",s.score);put("wrongSubmissions",s.wrongSubmissions);put("hintIds",JsonArray(s.usedHintIds.sorted().map(::JsonPrimitive)));put("durationMs",0)
  })}
 }
 private fun body(rows:List<SyncDeliveryEntity>)=buildJsonObject {put("operations",JsonArray(rows.map{Json.parseToJsonElement(it.payload)}))}
 private suspend fun retry(row:SyncDeliveryEntity,error:String){val retries=(row.retries+1).coerceAtMost(30);val delay=minOf(6*60*60*1000L,1000L*(1L shl minOf(retries,20)));dao.delivery(row.copy(retries=retries,nextRetryAt=now()+delay,error=error))}
 private suspend fun merge(account:String,c:SyncChange) {
  val v=c.value;require(v["serverVersion"]?.jsonPrimitive?.long==c.version)
  val old=dao.replica(account,c.entityType,c.entityId);if(old!=null && old.serverVersion>c.version)return
  when(c.entityType) {
   "progress"->{uuid(c.entityId);require(v["puzzleId"]?.jsonPrimitive?.content==c.entityId);val score=v.getValue("bestScore").jsonPrimitive.int;val attempts=v.getValue("attempts").jsonPrimitive.int;require(score in 0..300 && attempts>=0)
    val local=db.games().progress(c.entityId);val type=local?.gameType ?: db.games().anyPuzzle(c.entityId)?.let{SessionCodec.json.decodeFromString<PuzzleDefinition>(it.definition).type} ?: "choice"
    db.games().saveProgress(PlayerProgressEntity(c.entityId,maxOf(local?.bestScore?:0,score),(local?.completed?:false)||v.getValue("completed").jsonPrimitive.boolean,maxOf(local?.attempts?:0,attempts),type,maxOf(local?.xp?:0,score)))
   }
   "achievement"->{require(v["achievementId"]?.jsonPrimitive?.content==c.entityId && c.entityId.length in 1..2000);db.games().award(AchievementEntity(c.entityId,now()))}
   "saved_word","preference"->Unit
   else->error("Unknown sync entity")
  }
  dao.replica(SyncReplicaEntity(account,c.entityType,c.entityId,c.version,v.toString()))
 }
 companion object {private fun uuid(value:String){require(UUID.fromString(value).toString()==value.lowercase())}}
}
