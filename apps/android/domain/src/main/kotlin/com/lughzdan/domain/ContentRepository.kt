package com.lughzdan.domain

import kotlinx.coroutines.flow.Flow

data class ContentPack(
 val id:String, val title:String, val version:Int, val downloadBytes:Long,
 val installedBytes:Long=0, val installedVersion:Int?=null,
 val description:String="", val license:String="", val sources:List<String> = emptyList(),
 val gameTypes:List<String> = emptyList(), val published:Boolean=true
)
enum class DownloadStatus { QUEUED, WAITING_FOR_WIFI, DOWNLOADING, VERIFYING, INSTALLING, PAUSED, FAILED, COMPLETED, CANCELLED }
data class ContentDownload(val packId:String,val status:DownloadStatus,val downloadedBytes:Long=0,val totalBytes:Long=0,val error:String?=null)
data class ContentPreferences(val wifiOnly:Boolean=true,val automaticDownloads:Boolean=false)
data class ContentStorage(val installedBytes:Long=0,val temporaryBytes:Long=0)
interface ContentRepository {
 val catalog:Flow<List<ContentPack>>
 val downloads:Flow<List<ContentDownload>>
 val preferences:Flow<ContentPreferences>
 val storage:Flow<ContentStorage>
 /** Null when cloud catalog is configured. Otherwise a user-readable reason. */
 val disabledReason:Flow<String?>
 suspend fun refresh()
 suspend fun details(packId:String):ContentPack?
 suspend fun download(packId:String,allowMetered:Boolean=false)
 suspend fun pause(packId:String)
 suspend fun resume(packId:String)
 suspend fun cancel(packId:String)
 suspend fun retry(packId:String)
 /** Deletes installed content only; player progress must be preserved. */
 suspend fun delete(packId:String)
 suspend fun cleanupTemporaryFiles()
 suspend fun setWifiOnly(value:Boolean)
 suspend fun setAutomaticDownloads(value:Boolean)
}
