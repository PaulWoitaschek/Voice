package voice.playback.session

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import voice.common.BookId
import voice.data.ChapterId

@Serializable
sealed interface MediaId {
  @Serializable
  @SerialName("root")
  data object Root : MediaId

  @Serializable
  @SerialName("book")
  data class Book(val id: BookId) : MediaId

  @Serializable
  @SerialName("chapter")
  data class Chapter(
    val bookId: BookId,
    val chapterId: ChapterId,
  ) : MediaId

  @Serializable
  @SerialName("recent")
  data object Recent : MediaId
}
