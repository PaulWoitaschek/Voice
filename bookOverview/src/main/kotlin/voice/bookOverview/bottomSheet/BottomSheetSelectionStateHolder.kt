package voice.bookOverview.bottomSheet

import voice.bookOverview.di.BookOverviewScope
import voice.data.Book
import javax.inject.Inject

@BookOverviewScope
class BottomSheetSelectionStateHolder
@Inject constructor() {

  internal var selectedBook: Book.Id? = null
}
