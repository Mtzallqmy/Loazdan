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
  val root=File(context.cacheDir,"literary-${System.nanoTime()}")
  try {
   val keys=context.assets.open("pack-public-keys.json").bufferedReader().use {Json.parseToJsonElement(it.readText()).jsonObject.mapValues {Base64.getDecoder().decode(it.value.jsonPrimitive.content)}}
   val installer=PackInstaller(db,root,ManifestVerifier(keys),103,directorySync={})
   suspend fun install(){BundledLiteraryPack.install(context,db,installer)}
   val repository=LocalGameRepository(context,db,false,recoverContent={install()})
   repository.initialize()
   val all=repository.puzzles.first();assertEquals(168,all.size)
   assertEquals(48,all.count(LiteraryWords::supports))
   val p=all.first {LiteraryWords.supports(it)&&it.difficulty==1&&it.payload.variant==LiteraryWords.COMPLETE}
   val id=repository.start(p.puzzleId)
   val hint=p.hints.first {it.id.startsWith("letter-")}
   repository.act(id,PlayerAction.UseHint("hint",hint.id))
   install();assertEquals(168,repository.puzzles.first().size)
   val restored=LocalGameRepository(context,db,false,recoverContent={install()})
   restored.initialize()
   val session=restored.observeSession(id).first()!!.session
   assertEquals(SessionStatus.PAUSED,session.status);assertTrue(hint.id in session.usedHintIds)
   restored.act(id,PlayerAction.Resume("resume"))
   restored.act(id,PlayerAction.InputAnswer("input",p.payload.acceptedAnswers.first()))
   val won=restored.act(id,PlayerAction.Submit("submit"));assertEquals(SessionStatus.WON,won.status)
   val progress=restored.progress.first()
   restored.act(id,PlayerAction.Submit("repeat"));assertEquals(progress,restored.progress.first())
  } finally {db.close();root.deleteRecursively()}
 }
}
