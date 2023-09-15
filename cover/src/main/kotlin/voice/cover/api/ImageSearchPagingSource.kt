package voice.cover.api

import androidx.paging.PagingSource
import androidx.paging.PagingState
import java.io.IOException
import kotlinx.serialization.SerializationException
import retrofit2.HttpException

internal class ImageSearchPagingSource(
  private val api: CoverApi,
  private val query: String,
) : PagingSource<ImageSearchParams, SearchResponse.ImageResult>() {

  override fun getRefreshKey(state: PagingState<ImageSearchParams, SearchResponse.ImageResult>): ImageSearchParams? = null

  private suspend fun freshSearchParams(): ImageSearchParams? {
    val auth = api.token(query) ?: return null
    return ImageSearchParams("/i.js", auth)
  }

  override suspend fun load(params: LoadParams<ImageSearchParams>): LoadResult<ImageSearchParams, SearchResponse.ImageResult> {
    if (query.isBlank()) {
      return LoadResult.Page(
        prevKey = null,
        nextKey = null,
        data = emptyList(),
      )
    }
    return try {
      val searchParams = params.key ?: freshSearchParams()
        ?: return LoadResult.Error(IOException("No params"))

      val response = api.search(
        query = query,
        auth = searchParams.auth,
        url = searchParams.url,
      )
      val nextKey = if (response.next != null) {
        ImageSearchParams(url = response.next, auth = searchParams.auth)
      } else {
        null
      }
      LoadResult.Page(
        prevKey = null,
        nextKey = nextKey,
        data = response.results,
      )
    } catch (e: Exception) {
      if (e is HttpException || e is IOException || e is SerializationException) {
        LoadResult.Error(e)
      } else {
        throw e
      }
    }
  }
}
