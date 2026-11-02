package com.lughzdan.data
import android.content.Context
import androidx.work.*
import com.lughzdan.network.CatalogClient
import com.lughzdan.network.NetworkResult
/** Explicitly opt-in catalog probe. Never gates gameplay or claims a pack was downloaded. */
class CatalogRefreshWorker(context:Context,parameters:WorkerParameters):CoroutineWorker(context,parameters) {
 override suspend fun doWork():Result {
  val baseUrl=inputData.getString("baseUrl") ?: return Result.failure()
  if(!baseUrl.startsWith("https://"))return Result.failure()
  return when(CatalogClient.create(baseUrl).games()) {
   is NetworkResult.Success->Result.success()
   NetworkResult.Unavailable->if(runAttemptCount<3)Result.retry() else Result.failure()
  }
 }
}
