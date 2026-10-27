package com.lughzdan.network
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class CatalogClientTest {
 @Test fun malformedResponseBecomesUnavailable()=runBlocking {
  val client=CatalogClient(object:CatalogApi {
   override suspend fun games():CatalogPage=Json.decodeFromString<CatalogPage>("{\"items\":[{\"id\":\"game\"}]}")
  })
  assertEquals(NetworkResult.Unavailable,client.games())
 }
 @Test fun unavailableNetworkDoesNotEscape()=runBlocking {
  val client=CatalogClient(object:CatalogApi { override suspend fun games():CatalogPage=throw java.io.IOException("offline") })
  assertEquals(NetworkResult.Unavailable,client.games())
 }
 @Test fun cancellationIsNotConvertedToNetworkError()=runBlocking {
  val client=CatalogClient(object:CatalogApi { override suspend fun games():CatalogPage=throw CancellationException("cancelled") })
  try { client.games();fail("Cancellation swallowed") } catch(_:CancellationException) { }
 }
 @Test fun validResponseMapsToSuccess()=runBlocking {
  val items=listOf(CatalogGameDto("game","تحدي المفردات"))
  val client=CatalogClient(object:CatalogApi { override suspend fun games()=CatalogPage(items) })
  assertEquals(NetworkResult.Success(items),client.games())
 }
}
