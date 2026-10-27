package com.lughzdan.network
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.MediaType.Companion.toMediaType
import retrofit2.Retrofit
import retrofit2.converter.kotlinx.serialization.asConverterFactory
import retrofit2.http.GET
import java.util.concurrent.TimeUnit
@Serializable data class CatalogGameDto(val id:String,val title:String)
@Serializable data class CatalogPage(val items:List<CatalogGameDto>)
interface CatalogApi { @GET("api/v1/games") suspend fun games():CatalogPage }
sealed interface NetworkResult<out T> { data class Success<T>(val value:T):NetworkResult<T>; data object Unavailable:NetworkResult<Nothing> }
class CatalogClient(private val api:CatalogApi) {
 suspend fun games():NetworkResult<List<CatalogGameDto>> = try { NetworkResult.Success(api.games().items) } catch(e:kotlinx.coroutines.CancellationException) { throw e } catch(_:java.io.IOException) { NetworkResult.Unavailable } catch(_:retrofit2.HttpException) { NetworkResult.Unavailable }
 companion object {
  fun create(baseUrl:String):CatalogClient {
   require(baseUrl.startsWith("https://"))
   val http=OkHttpClient.Builder().connectTimeout(10,TimeUnit.SECONDS).readTimeout(15,TimeUnit.SECONDS).callTimeout(25,TimeUnit.SECONDS).retryOnConnectionFailure(false).build()
   return CatalogClient(Retrofit.Builder().baseUrl(baseUrl).client(http).addConverterFactory(Json{ignoreUnknownKeys=true}.asConverterFactory("application/json".toMediaType())).build().create(CatalogApi::class.java))
  }
 }
}
