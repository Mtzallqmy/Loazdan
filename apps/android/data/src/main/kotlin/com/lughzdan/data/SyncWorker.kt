package com.lughzdan.data

import android.content.Context
import androidx.work.*

/** Optional trusted application integration. Apps without login do not implement this interface. */
interface ProgressSyncOwner {fun progressSync():ProgressSync?}
class SyncWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters) {
 override suspend fun doWork():Result {
  val sync=(applicationContext as? ProgressSyncOwner)?.progressSync() ?: return Result.success()
  return try {
   repeat(3) {
    val result=sync.syncOnce()
    when(result.state) {
     "SIGNED_OUT","MERGE_REQUIRED","ACCOUNT_CHANGED","AUTH_REQUIRED","SNAPSHOT_REQUIRED"->return Result.success()
     "PUSH_FAILED","PULL_FAILED"->return Result.retry()
    }
    if(!result.hasMore)return Result.success()
   }
   Result.retry()
  }catch(e:kotlinx.coroutines.CancellationException){throw e}catch(_:Exception){Result.failure()}
 }
 companion object {
  /** Call after login + device registration and again when new receipts need delivery. */
  suspend fun schedule(context:Context,auth:AuthSessionProvider):Boolean {
   if(auth.current()==null || (context.applicationContext as? ProgressSyncOwner)?.progressSync()==null)return false
   val request=OneTimeWorkRequestBuilder<SyncWorker>().setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build()).setBackoffCriteria(BackoffPolicy.EXPONENTIAL,30,java.util.concurrent.TimeUnit.SECONDS).build()
   WorkManager.getInstance(context).enqueueUniqueWork("progress-sync",ExistingWorkPolicy.KEEP,request)
   return true
  }
 }
}
