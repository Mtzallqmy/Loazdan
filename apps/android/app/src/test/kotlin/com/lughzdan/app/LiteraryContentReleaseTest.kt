package com.lughzdan.app

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.lughzdan.data.*
import com.lughzdan.content.ManifestVerifier
import com.lughzdan.db.LughzdanDatabase
import com.lughzdan.engine.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.*
import org.junit.Assert.*
import org.junit.Assume.assumeFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File
import java.util.Base64

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[26],application=android.app.Application::class)
class LiteraryContentReleaseTest {
 @Test fun signedPackInstallsOfflineAndRestoresAlongsideOpeningPack()=runBlocking {
  assumeFalse(BuildConfig.DEBUG)
  val context=ApplicationProvider.getApplicationContext<Context>()
  val db=Room.inMemoryDatabaseBuilder(context,LughzdanDatabase::class.java).allowMainThreadQueries().build()
  val root=File(context.filesDir,"packs")
  try {
   val keys=context.assets.open("pack-public-keys.json").bufferedReader().use {Json.parseToJsonElement(it.readText()).jsonObject.mapValues {Base64.getDecoder().decode(it.value.jsonPrimitive.content)}}
   val installer=PackInstaller(db,root,ManifestVerifier(keys),104,directorySync={})
   val sourceRoot=generateSequence(File(".").canonicalFile){it.parentFile}.first {File(it,"packs/literary-words-48/manifest.json").exists()}
   val oldRaw=File(sourceRoot,"packs/literary-words-48/manifest.json").readBytes()
   val oldManifest=installer.verify(oldRaw)
   installer.chunks.mkdirs()
   oldManifest.files.forEach {file->
    val bytes=if(file.kind=="puzzles")File(sourceRoot,"packs/literary-words-48/puzzles.jsonl").readBytes()
      else context.assets.open("literary/"+file.path).use {it.readBytes()}
    File(installer.chunks,file.sha256).writeBytes(bytes)
   }
   installer.install(oldRaw,"ديوان الألغاز")
   val before=LocalGameRepository(context,db,false)
   before.initialize()
   assertEquals(168,before.atlasCatalog.first().size)
   val p=before.atlasCatalog.first().first {LiteraryWords.supports(it)&&it.difficulty==1&&it.payload.variant==LiteraryWords.COMPLETE}
   val id=before.start(p.puzzleId)
   val hint=p.hints.first {it.id.startsWith("letter-")}
   before.act(id,PlayerAction.UseHint("hint",hint.id))
   val oldSession=before.observeSession(id).first()!!.session
   assertEquals(2,oldSession.packVersion)
   suspend fun install(){BundledLiteraryPack.install(context,db,installer)}
   val restored=LocalGameRepository(context,db,false,recoverContent={install()})
   restored.initialize()
   val all=restored.atlasCatalog.first();assertEquals(600,all.size)
   assertTrue(all.count(LiteraryWords::supports)>=48)
   install();assertEquals(600,restored.atlasCatalog.first().size)
   val visuals=all.filter {it.type=="four_pictures" && it.tags.any {tag->tag.startsWith("supersedes:")}}
   assertEquals(4,visuals.size)
   for(visual in visuals) {
    val visualId=restored.start(visual.puzzleId)
    visual.payload.images.forEach {image->
     val bytes=restored.readImage(visualId,image.id)!!
     assertTrue(bytes.isNotEmpty())
     assertEquals(image.sha256,java.security.MessageDigest.getInstance("SHA-256").digest(bytes).joinToString(""){"%02x".format(it)})
    }
   }

   val session=restored.observeSession(id).first()!!.session
   assertEquals(SessionStatus.PAUSED,session.status);assertTrue(hint.id in session.usedHintIds)
   assertEquals(oldSession.copy(status=SessionStatus.PAUSED),session)
   assertEquals(p,restored.observeSession(id).first()!!.puzzle)
   restored.act(id,PlayerAction.Resume("resume"))
   restored.act(id,PlayerAction.InputAnswer("input",p.payload.acceptedAnswers.first()))
   val won=restored.act(id,PlayerAction.Submit("submit"));assertEquals(SessionStatus.WON,won.status)
   val progress=restored.progress.first()
   restored.act(id,PlayerAction.Submit("repeat"));assertEquals(progress,restored.progress.first())
  } finally {db.close();root.deleteRecursively()}
 }
}
