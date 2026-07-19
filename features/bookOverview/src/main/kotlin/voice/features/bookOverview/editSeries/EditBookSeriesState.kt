package voice.features.bookOverview.editSeries

import voice.core.data.BookId

internal data class EditBookSeriesState(
  val bookId: BookId,
  val author: String?,
  val currentSeries: String,
  val currentPart: String,
  val suggestedSeries: List<String>,
)
