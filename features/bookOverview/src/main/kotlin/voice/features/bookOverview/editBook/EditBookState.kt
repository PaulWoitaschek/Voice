package voice.features.bookOverview.editBook

import voice.core.audiobookshelf.ServerEdit
import voice.core.data.BookId
import voice.core.data.isRemote

internal data class EditBookState(
  val title: String,
  val author: String,
  val bookId: BookId,
  val cover: String?,
  val series: String = "",
  val part: String = "",
  val saving: Boolean = false,
  val serverResult: ServerEdit? = null,
  val allSeries: List<NameSuggestion> = emptyList(),
  val initialTitle: String = title,
  val initialAuthor: String = author,
  val initialSeries: String = series,
  val initialPart: String = part,
) {

  val remote: Boolean = bookId.isRemote

  val confirmButtonEnabled: Boolean = title.trim().isNotEmpty()

  val suggestions: List<NameSuggestion> =
    if (series == initialSeries) emptyList() else allSeries.matching(series)
}
