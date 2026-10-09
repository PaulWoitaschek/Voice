package voice.core.audiobookshelf.api

import kotlinx.serialization.json.JsonObject
import retrofit2.Call
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

internal interface AudiobookshelfApi {

  @GET("status")
  suspend fun status(): Response<StatusResponse>

  @Headers("x-return-tokens: true")
  @POST("login")
  suspend fun login(@Body request: LoginRequest): LoginResponse

  @POST("auth/refresh")
  fun refresh(@Header("x-refresh-token") refreshToken: String): Call<LoginResponse>

  @POST("logout")
  suspend fun logout(@Header("x-refresh-token") refreshToken: String): Response<Unit>

  @GET("api/me")
  suspend fun me(): AbsUser

  @GET("api/libraries")
  suspend fun libraries(): LibrariesResponse

  @GET("api/libraries/{id}/items")
  suspend fun libraryItems(
    @Path("id") libraryId: String,
    @Query("limit") limit: Int,
    @Query("page") page: Int,
    @Query("minified") minified: Int = 1,
  ): LibraryItemsResponse

  @POST("api/items/batch/get")
  suspend fun items(@Body request: BatchGetRequest): BatchGetResponse

  @PATCH("api/me/progress/{itemId}")
  suspend fun updateProgress(
    @Path("itemId") itemId: String,
    @Body update: ProgressUpdate,
  ): Response<Unit>

  @POST("api/me/item/{itemId}/bookmark")
  suspend fun addBookmark(
    @Path("itemId") itemId: String,
    @Body request: BookmarkRequest,
  ): Response<Unit>

  @PATCH("api/me/item/{itemId}/bookmark")
  suspend fun updateBookmark(
    @Path("itemId") itemId: String,
    @Body request: BookmarkRequest,
  ): Response<Unit>

  @DELETE("api/me/item/{itemId}/bookmark/{time}")
  suspend fun deleteBookmark(
    @Path("itemId") itemId: String,
    @Path("time") time: String,
  ): Response<Unit>

  @POST("api/items/{itemId}/play")
  suspend fun startSession(
    @Path("itemId") itemId: String,
    @Body request: StartSessionRequest,
  ): PlaybackSessionResponse

  @POST("api/session/{id}/sync")
  suspend fun syncSession(
    @Path("id") sessionId: String,
    @Body request: SessionSyncRequest,
  ): Response<Unit>

  /** An empty [request] only closes the session. With a position, the server would also take it as the progress. */
  @POST("api/session/{id}/close")
  suspend fun closeSession(
    @Path("id") sessionId: String,
    @Body request: JsonObject = JsonObject(emptyMap()),
  ): Response<Unit>
}
