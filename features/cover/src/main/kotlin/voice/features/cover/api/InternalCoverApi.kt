package voice.features.cover.api

import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.Url

interface InternalCoverApi {

  @GET("/")
  suspend fun auth(@Query("q") search: String): String

  @GET
  suspend fun search(
    @Url
    url: String,
    @Query("q")
    query: String,
    @Query("vqd")
    auth: String,
  ): SearchResponse
}
