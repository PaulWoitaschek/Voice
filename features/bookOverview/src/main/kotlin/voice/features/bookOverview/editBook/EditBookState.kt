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
  val showSeries: Boolean = false,
  val saving: Boolean = false,
  val serverResult: ServerEdit? = null,
  val allSeries: List<SeriesSuggestion> = emptyList(),
  val initialTitle: String = title,
  val initialAuthor: String = author,
  val initialSeries: String = series,
  val initialPart: String = part,
) {

  val remote: Boolean = bookId.isRemote

  val confirmButtonEnabled: Boolean = title.trim().isNotEmpty()

  val suggestions: List<SeriesSuggestion> = series.trim().let { typed ->
    if (typed.isEmpty() || series == initialSeries) {
      emptyList()
    } else {
      allSeries
        .filter { it.name.contains(typed, ignoreCase = true) && !it.name.equals(typed, ignoreCase = true) }
        .sortedByDescending { it.name.startsWith(typed, ignoreCase = true) }
        .take(3)
    }
  }
}

internal data class SeriesSuggestion(
  val name: String,
  val bookCount: Int,
)
