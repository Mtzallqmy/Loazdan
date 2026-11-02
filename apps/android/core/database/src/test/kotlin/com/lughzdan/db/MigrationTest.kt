package com.lughzdan.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk=[28])
class MigrationTest {
 @Test fun versionTwoPreservesPinnedContentAndPlayerData()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val filename="migration-v2-v3.db"
  context.deleteDatabase(filename)
  val old=context.openOrCreateDatabase(filename,Context.MODE_PRIVATE,null)
  // This frozen exported v2 schema is independent of current production DDL.
  val schema=JSONObject(javaClass.getResource("/room-v2.json")!!.readText()).getJSONObject("database")
  val entities=schema.getJSONArray("entities")
  for(i in 0 until entities.length()) {
   val entity=entities.getJSONObject(i)
   old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
   val indices=entity.optJSONArray("indices") ?: org.json.JSONArray()
   for(j in 0 until indices.length()) old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
  }
  old.execSQL("INSERT INTO installed_packs VALUES ('pack',7,'digest',123,1)")
  old.execSQL("INSERT INTO puzzles VALUES ('p',2,'pack','definition',3)")
  old.execSQL("INSERT INTO sessions VALUES ('s','p',2,'pinned-state',456)")
  old.execSQL("INSERT INTO progress VALUES ('p',80,1,4,'crossword',80)")
  old.execSQL("INSERT INTO achievements VALUES ('first_win',789)")
  old.execSQL("INSERT INTO sync_operations VALUES ('op','s','complete_session','payload')")
  old.version=2
  old.close()
  val db=Room.databaseBuilder(context,LughzdanDatabase::class.java,filename).addMigrations(LughzdanDatabase.MIGRATION_2_3,LughzdanDatabase.MIGRATION_3_4).allowMainThreadQueries().build()
  try {
   assertEquals("pinned-state",db.games().session("s")!!.state)
   assertEquals(2,db.games().session("s")!!.revision)
   assertEquals("definition",db.games().puzzle("p",2)!!.definition)
   assertEquals(7,db.games().pack("pack")!!.version)
   assertEquals(PlayerProgressEntity("p",80,true,4,"crossword",80),db.games().progress("p"))
   assertEquals(1,db.games().operationCount())
   assertTrue(db.content().heads().isEmpty())
   assertTrue(db.content().journals().isEmpty())
   db.content().account(SyncAccountEntity("account","device",1,null,false))
   assertEquals("device",db.content().account("account")!!.deviceId)
   db.openHelper.readableDatabase.query("SELECT awardedAt FROM achievements WHERE id='first_win'").use { assertTrue(it.moveToFirst());assertEquals(789L,it.getLong(0)) }
  } finally {db.close();context.deleteDatabase(filename)}
 }
 @Test fun versionThreeAddsVocabularyWithoutChangingContentSyncOrPlayerRows()=runBlocking {
  val context=ApplicationProvider.getApplicationContext<Context>()
  val filename="migration-v3-v4.db"
  context.deleteDatabase(filename)
  val old=context.openOrCreateDatabase(filename,Context.MODE_PRIVATE,null)
  val schema=JSONObject(javaClass.getResource("/room-v2.json")!!.readText()).getJSONObject("database")
  val entities=schema.getJSONArray("entities")
  for(i in 0 until entities.length()) {
   val entity=entities.getJSONObject(i)
   old.execSQL(entity.getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
   val indices=entity.optJSONArray("indices") ?: org.json.JSONArray()
   for(j in 0 until indices.length())old.execSQL(indices.getJSONObject(j).getString("createSql").replace("\${TABLE_NAME}",entity.getString("tableName")))
  }
  ContentSchema.statements.forEach(old::execSQL)
  old.execSQL("INSERT INTO installed_packs VALUES ('pack',7,'digest',123,1)")
  old.execSQL("INSERT INTO puzzles VALUES ('p',2,'pack','original-definition',3)")
  old.execSQL("INSERT INTO sessions VALUES ('s','p',2,'original-state',456)")
  old.execSQL("INSERT INTO progress VALUES ('p',80,1,4,'crossword',80)")
  old.execSQL("INSERT INTO achievements VALUES ('first_win',789)")
  old.execSQL("INSERT INTO sync_operations VALUES ('op','s','complete_session','payload')")
  old.execSQL("INSERT INTO pack_heads VALUES ('pack',7,6,7,'title','manifest',123)")
  old.execSQL("INSERT INTO pack_members VALUES ('pack',7,'p',2)")
  old.execSQL("INSERT INTO sync_accounts VALUES ('account','device',2,'cursor',1)")
  old.execSQL("INSERT INTO sync_delivery VALUES ('account','op','device',1,'original-payload','PENDING',2,42,NULL)")
  old.version=3;old.close()
  val db=Room.databaseBuilder(context,LughzdanDatabase::class.java,filename).addMigrations(LughzdanDatabase.MIGRATION_3_4).allowMainThreadQueries().build()
  try {
   assertEquals("original-state",db.games().session("s")!!.state)
   assertEquals("original-definition",db.games().puzzle("p",2)!!.definition)
   assertEquals(80,db.games().progress("p")!!.xp)
   assertEquals(1,db.games().operationCount())
   assertEquals(7,db.content().head("pack")!!.activeVersion)
   assertEquals(1,db.content().memberCount("pack",7))
   assertEquals("cursor",db.content().account("account")!!.cursor)
   assertEquals("original-payload",db.content().pending("account",100).single().payload)
   db.vocabulary().save(VocabularyEntity("word","كِتاب","meaning","source","page:1","كتب","category","[]","draft",true))
   assertEquals("كِتاب",db.vocabulary().entry("word")!!.word)
  } finally {db.close();context.deleteDatabase(filename)}
 }

}
