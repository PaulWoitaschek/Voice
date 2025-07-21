package voice.cover.api

import dev.zacsweers.metro.Inject
import voice.logging.core.Logger

@Inject
class CoverApi(private val api: InternalCoverApi) {

  internal suspend fun token(query: String): String? {
    Logger.d("query token")
    val response = api.auth(query)
    return "vqd=([\\d-]+)&".toRegex().find(response)?.groupValues?.get(1)
  }

  internal suspend fun search(
    query: String,
    auth: String,
    url: String = "/i.js",
  ): SearchResponse {
    Logger.d("search $query, url=$url")
    return api.search(
      query = query,
      auth = auth,
      url = url,
    )
  }
}
