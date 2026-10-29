package com.lughzdan.db

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Progress/session rows deliberately have no cascading foreign key to content.
@Entity(tableName="pack_heads") data class PackHeadEntity(@PrimaryKey val packId:String,val activeVersion:Int?,val previousVersion:Int?,val highWater:Int,val title:String,val manifest:String,val installedBytes:Long)
@Entity(tableName="pack_members",primaryKeys=["packId","version","puzzleId","revision"],indices=[Index(value=["puzzleId","revision"])]) data class PackMemberEntity(val packId:String,val version:Int,val puzzleId:String,val revision:Int)
@Entity(tableName="install_journal",primaryKeys=["packId","version"]) data class InstallJournalEntity(val packId:String,val version:Int,val phase:String,val manifest:String,val digest:String,val createdAt:Long)
@Entity(tableName="downloads") data class DownloadEntity(@PrimaryKey val packId:String,val version:Int,val state:String,val transferred:Long,val total:Long,val allowMetered:Boolean,val error:String?,val attempt:Int,val updatedAt:Long)
@Entity(tableName="sync_delivery",primaryKeys=["accountId","operationId"],indices=[Index(value=["accountId","deviceId","deviceSeq"],unique=true)]) data class SyncDeliveryEntity(val accountId:String,val operationId:String,val deviceId:String,val deviceSeq:Long,val payload:String,val status:String,val retries:Int,val nextRetryAt:Long,val error:String?)
@Entity(tableName="sync_accounts") data class SyncAccountEntity(@PrimaryKey val accountId:String,val deviceId:String,val nextSeq:Long,val cursor:String?,val mergeConfirmed:Boolean)
@Entity(tableName="sync_replica",primaryKeys=["accountId","entityType","entityId"]) data class SyncReplicaEntity(val accountId:String,val entityType:String,val entityId:String,val serverVersion:Long,val payload:String)

@Dao interface ContentDao {
 @Query("SELECT * FROM pack_heads") fun observeHeads():Flow<List<PackHeadEntity>>
 @Query("SELECT * FROM pack_heads") suspend fun heads():List<PackHeadEntity>
 @Query("SELECT * FROM pack_heads WHERE packId=:id") suspend fun head(id:String):PackHeadEntity?
 @Upsert suspend fun head(value:PackHeadEntity)
 @Insert(onConflict=OnConflictStrategy.IGNORE) suspend fun members(values:List<PackMemberEntity>)
 @Query("SELECT * FROM pack_members WHERE packId=:id AND version=:version") suspend fun members(id:String,version:Int):List<PackMemberEntity>
 @Query("SELECT COUNT(*) FROM pack_members WHERE packId=:id AND version=:version AND puzzleId=:puzzle AND revision=:revision") suspend fun membership(id:String,version:Int,puzzle:String,revision:Int):Int
 @Query("SELECT COUNT(*) FROM pack_members WHERE packId=:id AND version=:version") suspend fun memberCount(id:String,version:Int):Int
 @Upsert suspend fun journal(value:InstallJournalEntity)
 @Query("SELECT * FROM install_journal") suspend fun journals():List<InstallJournalEntity>
 @Query("SELECT * FROM install_journal WHERE packId=:id AND version=:version") suspend fun journal(id:String,version:Int):InstallJournalEntity?
 @Query("DELETE FROM install_journal WHERE packId=:id AND version=:version") suspend fun removeJournal(id:String,version:Int)
 @Query("SELECT * FROM downloads ORDER BY updatedAt DESC") fun observeDownloads():Flow<List<DownloadEntity>>
 @Query("SELECT * FROM downloads") suspend fun downloads():List<DownloadEntity>
 @Query("SELECT * FROM downloads WHERE packId=:id") suspend fun download(id:String):DownloadEntity?
 @Upsert suspend fun download(value:DownloadEntity)
 @Query("SELECT * FROM sync_operations WHERE operationId NOT IN(SELECT operationId FROM sync_delivery WHERE accountId=:account) ORDER BY operationId LIMIT :limit") suspend fun unboundOperations(account:String,limit:Int=100):List<SyncOperationEntity>
 @Query("SELECT * FROM sync_delivery WHERE accountId=:account AND status='PENDING' AND nextRetryAt<=:now ORDER BY deviceSeq LIMIT :limit") suspend fun pending(account:String,now:Long,limit:Int=100):List<SyncDeliveryEntity>
 @Upsert suspend fun delivery(value:SyncDeliveryEntity)
 @Query("SELECT * FROM sync_accounts WHERE accountId=:account") suspend fun account(account:String):SyncAccountEntity?
 @Upsert suspend fun account(value:SyncAccountEntity)
 @Upsert suspend fun replica(value:SyncReplicaEntity)
 @Query("SELECT * FROM sync_replica WHERE accountId=:account AND entityType=:type AND entityId=:id") suspend fun replica(account:String,type:String,id:String):SyncReplicaEntity?
 @Query("DELETE FROM pack_members WHERE packId=:id") suspend fun deleteMembers(id:String)
 @Query("DELETE FROM installed_packs WHERE packId=:id AND version=:version") suspend fun deleteInstalledVersion(id:String,version:Int)
 @Query("DELETE FROM installed_packs WHERE packId=:id") suspend fun deleteInstalled(id:String)
}

object ContentSchema {
 val statements=listOf(
 "CREATE TABLE IF NOT EXISTS pack_heads (packId TEXT NOT NULL, activeVersion INTEGER, previousVersion INTEGER, highWater INTEGER NOT NULL, title TEXT NOT NULL, manifest TEXT NOT NULL, installedBytes INTEGER NOT NULL, PRIMARY KEY(packId))",
 "CREATE TABLE IF NOT EXISTS pack_members (packId TEXT NOT NULL, version INTEGER NOT NULL, puzzleId TEXT NOT NULL, revision INTEGER NOT NULL, PRIMARY KEY(packId,version,puzzleId,revision))",
 "CREATE INDEX IF NOT EXISTS index_pack_members_puzzleId_revision ON pack_members(puzzleId,revision)",
 "CREATE TABLE IF NOT EXISTS install_journal (packId TEXT NOT NULL, version INTEGER NOT NULL, phase TEXT NOT NULL, manifest TEXT NOT NULL, digest TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(packId,version))",
 "CREATE TABLE IF NOT EXISTS downloads (packId TEXT NOT NULL, version INTEGER NOT NULL, state TEXT NOT NULL, transferred INTEGER NOT NULL, total INTEGER NOT NULL, allowMetered INTEGER NOT NULL, error TEXT, attempt INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(packId))",
 "CREATE TABLE IF NOT EXISTS sync_delivery (accountId TEXT NOT NULL, operationId TEXT NOT NULL, deviceId TEXT NOT NULL, deviceSeq INTEGER NOT NULL, payload TEXT NOT NULL, status TEXT NOT NULL, retries INTEGER NOT NULL, nextRetryAt INTEGER NOT NULL, error TEXT, PRIMARY KEY(accountId,operationId))",
 "CREATE UNIQUE INDEX IF NOT EXISTS index_sync_delivery_accountId_deviceId_deviceSeq ON sync_delivery(accountId,deviceId,deviceSeq)",
 "CREATE TABLE IF NOT EXISTS sync_accounts (accountId TEXT NOT NULL, deviceId TEXT NOT NULL, nextSeq INTEGER NOT NULL, cursor TEXT, mergeConfirmed INTEGER NOT NULL, PRIMARY KEY(accountId))",
 "CREATE TABLE IF NOT EXISTS sync_replica (accountId TEXT NOT NULL, entityType TEXT NOT NULL, entityId TEXT NOT NULL, serverVersion INTEGER NOT NULL, payload TEXT NOT NULL, PRIMARY KEY(accountId,entityType,entityId))"
 )
}
