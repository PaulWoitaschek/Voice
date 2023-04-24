package voice.cover.api

import kotlinx.serialization.Serializable

@Serializable
data class SearchResponse(
  val next: String?,
  val results: List<ImageResult>,
) {

  @Serializable
  data class ImageResult(
    val width: Int,
    val height: Int,
    val image: String,
    val thumbnail: String,
  )
}
