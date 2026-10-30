package com.lughzdan.domain

import kotlinx.coroutines.flow.Flow

/** An immutable copy of the displayed content, independent of installed pack lifetime. */
data class VocabularySnapshot(
 val id:String, val word:String, val meaning:String, val source:String,
 val sourceLocator:String, val root:String="", val category:String="",
 val linkedGameIds:List<String> = emptyList(), val contentStatus:String,
 val fixtureOnly:Boolean
)
data class VocabularyEntry(val snapshot:VocabularySnapshot,val favorite:Boolean=false,val reviewCount:Int=0,val lastReviewedAt:Long?=null)
interface VocabularyRepository {
 val entries:Flow<List<VocabularyEntry>>
 val catalog:Flow<List<VocabularySnapshot>>
 /** Re-saving the same ID preserves its original snapshot and personal state. */
 suspend fun save(snapshot:VocabularySnapshot)
 suspend fun addManual(word:String,meaning:String,root:String="",category:String="")
 suspend fun setFavorite(id:String,value:Boolean)
 /** A manual practice counter; this does not approve or validate content. */
 suspend fun recordReview(id:String)
 suspend fun remove(id:String)
}
