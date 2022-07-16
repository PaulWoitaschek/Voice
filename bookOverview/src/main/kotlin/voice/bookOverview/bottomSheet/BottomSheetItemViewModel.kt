package voice.bookOverview.bottomSheet

import voice.data.Book

interface BottomSheetItemViewModel {

  suspend fun items(bookId: Book.Id): List<BottomSheetItem>
  suspend fun onItemClicked(bookId: Book.Id, item: BottomSheetItem)
}
