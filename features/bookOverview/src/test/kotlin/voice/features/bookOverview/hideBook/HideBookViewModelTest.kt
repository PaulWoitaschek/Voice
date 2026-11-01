package voice.features.bookOverview.hideBook

import app.cash.turbine.test
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import voice.core.data.CustomMetadata
import voice.core.data.repo.BookRepository
import voice.features.bookOverview.book
import voice.features.bookOverview.bottomSheet.BottomSheetItem

class HideBookViewModelTest {

  private val book = book(name = "Dune")
  private val hiddenBook = book(hidden = true)

  private val repo =
    mockk<BookRepository> {
      coEvery { get(book.id) } returns book
      coEvery { get(hiddenBook.id) } returns hiddenBook
      coEvery { updateCustomMetadata(any(), any()) } just runs
    }

  private val bookHider = BookHider(repo)

  private val viewModel = HideBookViewModel(repo, bookHider)

  @Test
  fun `a book in the library can be hidden`() = runTest {
    assertEquals(listOf(BottomSheetItem.Hide), viewModel.items(book.id))
  }

  @Test
  fun `a hidden book can be shown again`() = runTest {
    assertEquals(listOf(BottomSheetItem.Show), viewModel.items(hiddenBook.id))
  }

  @Test
  fun `hiding a book marks it and tells the library, so it can offer an undo`() = runTest {
    bookHider.hidden.test {
      viewModel.onItemClick(book.id, BottomSheetItem.Hide)

      assertEquals(HiddenBook(book.id, "Dune"), awaitItem())
    }
    val update = slot<(CustomMetadata) -> CustomMetadata>()
    coVerify { repo.updateCustomMetadata(book.id, capture(update)) }
    assertEquals(true, update.captured(book.custom).hidden)
  }

  @Test
  fun `showing a book takes the mark off`() = runTest {
    viewModel.onItemClick(hiddenBook.id, BottomSheetItem.Show)

    val update = slot<(CustomMetadata) -> CustomMetadata>()
    coVerify { repo.updateCustomMetadata(hiddenBook.id, capture(update)) }
    assertEquals(false, update.captured(hiddenBook.custom).hidden)
  }
}
