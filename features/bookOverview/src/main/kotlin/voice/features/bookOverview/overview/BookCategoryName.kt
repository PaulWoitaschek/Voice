package voice.features.bookOverview.overview

import androidx.annotation.StringRes
import voice.core.data.BookCategory
import voice.core.strings.R as StringsR

@get:StringRes
internal val BookCategory.nameRes: Int
  get() =
    when (this) {
      BookCategory.CURRENT -> StringsR.string.library_category_current_title
      BookCategory.NOT_STARTED -> StringsR.string.library_category_not_started_title
      BookCategory.FINISHED -> StringsR.string.library_category_completed_title
    }
