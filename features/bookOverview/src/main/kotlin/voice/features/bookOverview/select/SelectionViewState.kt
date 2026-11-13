package voice.features.bookOverview.select

import androidx.compose.runtime.Immutable
import voice.core.data.BookId
import voice.features.bookOverview.editBook.NameSuggestion

@Immutable
data class SelectionViewState(
  val ids: Set<BookId>,
  val allSelected: Boolean,
  val allHidden: Boolean,
)

@Immutable
data class SelectionEditViewState(
  val field: BookField,
  val ids: Set<BookId>,
  val value: String,
  val suggestions: List<NameSuggestion>,
  val saving: Boolean = false,
  val saved: Boolean = false,
)
