package voice.features.bookOverview.series

import androidx.compose.runtime.Immutable
import voice.core.data.BookId
import voice.features.bookOverview.overview.BookOverviewItemViewState

@Immutable
data class SeriesViewState(
  val key: String,
  val name: String,
  val author: String?,
  val covers: List<String?>,
  val bookCount: Int,
  val finishedCount: Int,
  val action: Action?,
  val books: List<Book>,
) {

  @Immutable
  data class Action(
    val bookId: BookId,
    val part: String?,
    val continues: Boolean,
    val playing: Boolean,
  )

  @Immutable
  data class Book(
    val book: BookOverviewItemViewState,
    val part: String?,
    val finished: Boolean,
    val current: Boolean,
    val playing: Boolean,
  )
}
