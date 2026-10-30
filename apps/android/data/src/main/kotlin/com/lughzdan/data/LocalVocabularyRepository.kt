package com.lughzdan.data

import com.lughzdan.db.*
import com.lughzdan.domain.*
import com.lughzdan.engine.SessionCodec
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import java.util.UUID

class LocalVocabularyRepository(private val db:LughzdanDatabase,override val catalog:Flow<List<VocabularySnapshot>> = flowOf(emptyList())):VocabularyRepository {
 private val dao=db.vocabulary()
 override val entries=dao.observe().map { rows->rows.map { row->
  VocabularyEntry(VocabularySnapshot(row.id,row.word,row.meaning,row.source,row.sourceLocator,row.root,row.category,
   SessionCodec.json.decodeFromString<List<String>>(row.linkedGameIds),row.contentStatus,row.fixtureOnly),row.favorite,row.reviewCount,row.lastReviewedAt)
 } }
 override suspend fun save(snapshot:VocabularySnapshot) {
  require(snapshot.id.isNotBlank() && snapshot.word.isNotBlank() && snapshot.meaning.isNotBlank())
  dao.save(VocabularyEntity(snapshot.id,snapshot.word,snapshot.meaning,snapshot.source,snapshot.sourceLocator,
   snapshot.root,snapshot.category,SessionCodec.json.encodeToString(snapshot.linkedGameIds),snapshot.contentStatus,snapshot.fixtureOnly))
 }
 override suspend fun addManual(word:String,meaning:String,root:String,category:String) {
  require(word.isNotBlank() && meaning.isNotBlank())
  val id="user_note:${UUID.randomUUID()}"
  save(VocabularySnapshot(id,word,meaning,"user_note",id,root,category,emptyList(),"unreviewed",false))
 }
 override suspend fun setFavorite(id:String,value:Boolean)=dao.favorite(id,value)
 override suspend fun recordReview(id:String)=dao.review(id,System.currentTimeMillis())
 override suspend fun remove(id:String)=dao.remove(id)
}
