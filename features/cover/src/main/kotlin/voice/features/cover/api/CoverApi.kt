package voice.features.cover.api

import dev.zacsweers.metro.Inject

@Inject
class CoverApi(private val api: InternalCoverApi) {

  internal suspend fun token(query: String): String? {
    val response = api.auth(query)
    return "vqd=([\\d-]+)&".toRegex().find(response)?.groupValues?.get(1)
  }

  internal suspend fun search(
    query: String,
    auth: String,
    url: String = "/i.js",
  ): SearchResponse {
    return api.search(
      query = query,
      auth = auth,
      url = url,
    )
  }
}
