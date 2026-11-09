package voice.features.bookOverview.author

import androidx.compose.runtime.Immutable
import voice.features.bookOverview.overview.BookOverviewItemViewState
import voice.features.bookOverview.overview.SeriesStackViewState

@Immutable
data class AuthorViewState(
  val name: String?,
  val bookCount: Int,
  val finishedCount: Int,
  val info: AuthorInfo?,
  val entries: List<Entry>,
) {

  @Immutable
  sealed interface Entry {
    val key: String

    data class Book(
      val book: BookOverviewItemViewState,
      val finished: Boolean,
    ) : Entry {
      override val key: String
        get() = book.id.value
    }

    data class Series(val series: SeriesStackViewState) : Entry {
      // a book id is a URI or starts with abs:, so this never clashes with one
      override val key: String
        get() = "series:${series.key}"
    }
  }
}
