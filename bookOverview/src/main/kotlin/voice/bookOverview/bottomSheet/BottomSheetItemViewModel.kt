package voice.bookOverview.bottomSheet

import voice.data.BookId

interface BottomSheetItemViewModel {

  suspend fun items(bookId: BookId): List<BottomSheetItem>
  suspend fun onItemClick(
    bookId: BookId,
    item: BottomSheetItem,
  )
}
