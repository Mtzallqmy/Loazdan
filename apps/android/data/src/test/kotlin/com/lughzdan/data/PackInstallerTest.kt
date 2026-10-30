package com.lughzdan.data

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lughzdan.content.*
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
import java.io.File
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.Signature
import java.util.Base64

/** All content and signing keys here are generated solely for tests; no real published pack. */
@RunWith(RobolectricTestRunner::class) @Config(sdk=[28])
class PackInstallerTest {
 private lateinit var db:LughzdanDatabase
 private lateinit var root:File
 private val keys=KeyPairGenerator.getInstance("Ed25519").generateKeyPair()
 private val pack="5637c78f-fe7e-528c-9763-9450426f7f18"
 private val other="5637c78f-fe7e-528c-9763-9450426f7f20"
 private val ref="5637c78f-fe7e-528c-9763-9450426f7f19"
 private val puzzleId="5637c78f-fe7e-528c-9763-9450426f7f21"
 private val json=Json {encodeDefaults=true}
 private class Interrupted(val phase:String):RuntimeException(phase)
 private fun installer(checkpoint:(String)->Unit={})=PackInstaller(db,root,ManifestVerifier(mapOf("runtime-test" to keys.public.encoded.takeLast(32).toByteArray())),checkpoint=checkpoint)
 @Before fun setup(){val context=ApplicationProvider.getApplicationContext<Context>();db=Room.inMemoryDatabaseBuilder(context,LughzdanDatabase::class.java).allowMainThreadQueries().build();root=File(context.cacheDir,"pack-test-${System.nanoTime()}").apply{mkdirs()}}
 @After fun cleanup(){db.close();root.deleteRecursively()}
 private fun hash(bytes:ByteArray)=MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)}
 private fun puzzle(revision:Int=1,status:String="published",fixture:Boolean=false,id:String=puzzleId)=PuzzleDefinition(1,id,revision,"5637c78f-fe7e-528c-9763-9450426f7f22","choice","words",1,1,"arabic_basic_v1","Runtime test puzzle",status,fixture,listOf(ref),"Runtime test explanation",emptyList(),ChoicePayload("Question",listOf(Option("a","A"),Option("b","B"),Option("c","C"),Option("d","D")),"a"))
 // These fixture values need only object-key sorting: no floats, negative zero or escaped controls.
 private fun canonical(e:JsonElement):JsonElement=when(e){is JsonObject->JsonObject(e.toSortedMap().mapValues{canonical(it.value)});is JsonArray->JsonArray(e.map(::canonical));else->e}
 private fun signed(version:Int,revision:Int=version,id:String=pack,status:String="published",fixture:Boolean=false,deps:List<Dependency> = emptyList(),title:String="Runtime test puzzle",visual:VisualImage?=null,minEngine:Int=1):ByteArray {
  val p=puzzle(revision,status,fixture,if(id==pack)puzzleId else other).copy(title=title).let { if(visual==null) it else it.copy(payload=it.payload.copy(images=listOf(visual))) }
  val payload=(json.encodeToString(p)+"\n").toByteArray();val rights="Test-only rights notice\n".toByteArray()
  val entries=listOf("data/puzzles.jsonl" to payload,"rights/source.txt" to rights)+(if(visual!=null) listOf("media/test.png" to byteArrayOf(1,2,3)) else emptyList())
  val files=entries.map{(path,bytes)->val h=hash(bytes);File(root,"chunks/$h").apply{parentFile!!.mkdirs();writeBytes(bytes)};ManifestFile(path,if(path.startsWith("data/"))"puzzles" else if(path.startsWith("media/"))"media" else "rights",h,bytes.size.toLong(),listOf(Chunk(h,bytes.size.toLong())))}
  val manifest=SignedManifest(1,id,version,1,minEngine,"ar","2026-10-26T00:00:00Z",files,files.sumOf{it.bytes},files.sumOf{it.bytes},deps,listOf(ref),"runtime-test","",if(visual==null) emptyList() else listOf(Asset(other,"media/test.png",ref,"Test image")),listOf(SourceReference(ref,pack,"Runtime source","Test author","test locator","1","CC0","Test attribution","rights/source.txt")))
  val obj=json.parseToJsonElement(json.encodeToString(manifest)).jsonObject
  val bytes=canonical(JsonObject(obj.filterKeys{it!="signature"})).toString().toByteArray()
  val signer=Signature.getInstance("Ed25519");signer.initSign(keys.private);signer.update(bytes)
  return JsonObject(obj+("signature" to JsonPrimitive(Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign())))).toString().toByteArray()
 }
 private suspend fun rejected(block:suspend ()->Unit){try{block();fail("Expected rejection")}catch(_:IllegalArgumentException){}catch(_:IllegalStateException){}}
 private suspend fun interrupted(phase:String,bytes:ByteArray){try{installer{if(it==phase)throw Interrupted(it)}.install(bytes,"Test pack");fail("Checkpoint not reached: $phase")}catch(e:Interrupted){assertEquals(phase,e.phase)}}
 private suspend fun progress(){db.games().saveProgress(PlayerProgressEntity(puzzleId,100,true,2,"choice",100));db.games().enqueue(SyncOperationEntity("receipt","session","complete_session","preserved"))}
 @Test fun acceptsCurrentEngineManifest()=runBlocking {
  installer().install(signed(1,minEngine=3),"Engine 3")
  assertEquals(1,db.content().head(pack)!!.activeVersion)
 }
 @Test fun rejectsImageMetadataThatDoesNotMatchSignedMedia()=runBlocking {
  val image=VisualImage("test","media/test.png","Test image","clue",ref,"CC0",1,1,hash(byteArrayOf(1,2,3)))
  listOf(image.copy(sha256="0".repeat(64)),image.copy(sourceRef=other),image.copy(license="Unlicensed"),image.copy(alt="Changed description"),image.copy(assetPath="media/missing.png")).forEachIndexed {index,bad ->
   rejected {installer().install(signed(index+1,visual=bad),"Unbound image")}
   assertNull(db.content().head(pack)?.activeVersion)
   assertTrue(db.games().puzzles().isEmpty())
  }
  installer().install(signed(6,visual=image,minEngine=3),"Bound image")
  assertEquals(6,db.content().head(pack)!!.activeVersion)
 }
 @Test fun signedPublishedSourceCanProduceSyncOperation()=runBlocking {
  installer().install(signed(1),"Test")
  val terminal=GameSession("5637c78f-fe7e-528c-9763-9450426f7f24",puzzleId,1,packVersion=1,packId=pack,status=SessionStatus.WON,score=100)
  db.games().enqueue(SyncOperationEntity("5637c78f-fe7e-528c-9763-9450426f7f25",terminal.id,"complete_session",SessionCodec.encode(terminal)))
  var sent:JsonObject?=null
  val transport=object:SyncTransport {
   override suspend fun push(session:AuthSession,body:JsonObject):SyncPushReply {sent=body;return SyncPushReply(listOf(SyncPushResult("5637c78f-fe7e-528c-9763-9450426f7f25","accepted")))}
   override suspend fun pull(session:AuthSession,cursor:String?,limit:Int)=SyncPullReply(emptyList(),"0",false)
  }
  val sync=ProgressSync(db,AuthSessionProvider {AuthSession("test-account","5637c78f-fe7e-528c-9763-9450426f7f26","test-only-token")},transport)
  sync.confirmMerge();assertEquals(1,sync.syncOnce().pushed)
  val payload=sent!!.getValue("operations").jsonArray.single().jsonObject.getValue("payload").jsonObject
  assertEquals(puzzleId,payload.getValue("puzzleId").jsonPrimitive.content)
  assertEquals(1,db.games().operationCount())
 }
 @Test fun stagedIndexIsInvisibleUntilAtomicPointerCommit()=runBlocking {
  val bytes=signed(1);val seen=mutableSetOf<String>()
  installer {phase->seen+=phase;runBlocking {if(phase!="COMMITTED"){assertTrue(db.games().puzzles().isEmpty());assertNull(db.games().pack(pack));assertNull(db.content().head(pack)?.activeVersion)}else{assertEquals(1,db.games().puzzles().size);assertEquals(1,db.content().head(pack)!!.activeVersion)}}}.install(bytes,"Test")
  assertEquals(setOf("JOURNALED","MATERIALIZED","INDEXED","RENAMED","COMMITTED"),seen)
 }
 @Test fun everyCrashCheckpointRetainsOldVisibilityAndProgress()=runBlocking {
  installer().install(signed(1),"Test");progress()
  // Separate increasing versions avoid reusing a prepared immutable journal.
  listOf("JOURNALED","MATERIALIZED","INDEXED","RENAMED","COMMITTED").forEachIndexed {index,phase->
   val version=index+2;val bytes=signed(version);val old=db.content().head(pack)!!.activeVersion
   interrupted(phase,bytes)
   assertEquals(if(phase=="COMMITTED")version else old,db.content().head(pack)!!.activeVersion)
   assertEquals(if(phase=="COMMITTED")version else old,db.games().latestPuzzle(puzzleId)!!.revision)
   assertEquals(100,db.games().progress(puzzleId)!!.xp);assertEquals(1,db.games().operationCount())
   // Retry the exact signed bytes; this must be idempotent after a commit and resume otherwise.
   installer().install(bytes,"Test");assertEquals(version,db.content().head(pack)!!.activeVersion)
  }
 }
 @Test fun recoveryAfterRenameCommitsAndPreservesPinnedOldVersion()=runBlocking {
  installer().install(signed(1),"Test");progress();interrupted("RENAMED",signed(2))
  assertEquals(1,db.content().head(pack)!!.activeVersion);installer().recover()
  assertEquals(2,db.content().head(pack)!!.activeVersion);assertEquals(1,db.content().head(pack)!!.previousVersion)
  assertNotNull(db.games().pinnedPack(pack,1));assertNotNull(db.games().puzzle(puzzleId,1));assertEquals(100,db.games().progress(puzzleId)!!.xp)
 }
 @Test fun damagedUpgradeRollsBackLocallyButRetainsHighWater()=runBlocking {
  val old=signed(1);installer().install(old,"Test");installer().install(signed(2),"Test");progress()
  File(root,"versions/$pack/2/data/puzzles.jsonl").writeText("damaged")
  installer().recover();val head=db.content().head(pack)!!
  assertEquals(1,head.activeVersion);assertEquals(2,head.highWater);assertEquals(1,db.games().latestPuzzle(puzzleId)!!.revision)
  rejected{installer().install(old,"Test")};assertEquals(100,db.games().progress(puzzleId)!!.xp)
 }
 @Test fun deleteRetainsProgressAndBlocksRemovingRequiredPack()=runBlocking {
  installer().install(signed(1),"Base");progress()
  installer().install(signed(1,id=other,deps=listOf(Dependency(pack,1))),"Dependent")
  rejected{installer().delete(pack)};assertEquals(1,db.content().head(pack)!!.activeVersion)
  installer().delete(other);installer().delete(pack)
  assertNull(db.content().head(pack)!!.activeVersion);assertTrue(db.games().puzzles().isEmpty());assertFalse(File(root,"versions/$pack").exists())
  assertEquals(100,db.games().progress(puzzleId)!!.xp);assertEquals(1,db.games().operationCount());assertEquals(1,db.content().head(pack)!!.highWater)
 }
 @Test fun samePublishedVersionCannotChangeSignedBytes()=runBlocking {
  installer().install(signed(1),"Test");rejected{installer().install(signed(1,title="Changed immutable version"),"Test")}
  assertEquals("Runtime test puzzle",json.decodeFromString<PuzzleDefinition>(db.games().latestPuzzle(puzzleId)!!.definition).title)
 }
 @Test fun rejectsDraftAndFixtureContentBeforeMakingItPlayable()=runBlocking {
  rejected{installer().install(signed(1,status="draft"),"Draft")};assertTrue(db.games().puzzles().isEmpty());assertNull(db.content().head(pack)?.activeVersion)
  // Fresh version because a signed journal's version bytes are immutable even after import rejection.
  rejected{installer().install(signed(2,fixture=true),"Fixture")};assertTrue(db.games().puzzles().isEmpty());assertNull(db.content().head(pack)?.activeVersion)
 }
}
