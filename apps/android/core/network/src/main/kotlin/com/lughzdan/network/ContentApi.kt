package com.lughzdan.network
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.ResponseBody
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.*
import java.util.concurrent.TimeUnit

@Serializable data class RemotePack(val id:String,val title:String,val version:Int,val downloadBytes:Long,val installedBytes:Long,val minAppVersionCode:Int)
@Serializable data class RemotePacks(val items:List<RemotePack>,val nextCursor:String?=null)
@Serializable data class DownloadObject(val sha256:String,val url:String)
@Serializable data class DownloadLinks(val packId:String,val version:Int,val objects:List<DownloadObject>)
interface ContentApi {
 @GET("api/v1/content-packs") suspend fun packs(@Query("cursor")cursor:String?=null,@Query("limit")limit:Int=100,@Query("appVersionCode")appVersion:Int=3):RemotePacks
 @Streaming @GET("api/v1/content-packs/{id}/manifest") suspend fun manifest(@Path("id")id:String,@Query("version")version:Int):ResponseBody
 @GET("api/v1/content-packs/{id}/download") suspend fun downloads(@Path("id")id:String,@Query("version")version:Int):DownloadLinks
 companion object {
  fun create(baseUrl:String):ContentApi {
   require(baseUrl.startsWith("https://") && baseUrl.endsWith('/'))
   val http=OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS).readTimeout(30,TimeUnit.SECONDS).callTimeout(45,TimeUnit.SECONDS).retryOnConnectionFailure(false).followRedirects(false).build()
   return Retrofit.Builder().baseUrl(baseUrl).client(http).addConverterFactory(Json{ignoreUnknownKeys=true}.asConverterFactory("application/json".toMediaType())).build().create(ContentApi::class.java)
  }
 }
}
