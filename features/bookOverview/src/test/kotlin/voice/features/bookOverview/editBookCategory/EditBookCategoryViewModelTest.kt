package voice.features.bookOverview.editBookCategory

import androidx.datastore.core.DataStore
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.coVerifyOrder
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.playback.PlayerController
import voice.features.bookOverview.book
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.chapter

class EditBookCategoryViewModelTest {

  private val book = book(chapters = listOf(chapter(duration = 5_000), chapter(duration = 7_000)))

  private val repo = mockk<BookRepository> {
    coEvery { get(book.id) } returns book
    coEvery { updateBook(book.id, any()) } just Runs
  }

  private fun viewModel(
    currentBookStoreId: BookId? = null,
    playerController: PlayerController = mockk(relaxed = true),
  ): Pair<EditBookCategoryViewModel, PlayerController> {
    val currentBookStore = mockk<DataStore<BookId?>> {
      every { data } returns flowOf(currentBookStoreId)
    }
    val vm = EditBookCategoryViewModel(
      repo = repo,
      currentBookStore = currentBookStore,
      playerController = playerController,
    )
    return vm to playerController
  }

  @Test
  fun `MarkAsNotStarted on currently loaded book seeks player to 0 and pauses`() = runTest {
    val (vm, player) = viewModel(currentBookStoreId = book.id)

    vm.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsNotStarted)

    coVerifyOrder {
      player.setPosition(0L, book.chapters.first().id)
      player.pause()
    }
  }

  @Test
  fun `MarkAsCurrent on currently loaded book seeks player to position 1 and pauses`() = runTest {
    val (vm, player) = viewModel(currentBookStoreId = book.id)

    vm.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsCurrent)

    coVerifyOrder {
      player.setPosition(1L, book.chapters.first().id)
      player.pause()
    }
  }

  @Test
  fun `MarkAsCompleted on currently loaded book seeks player to last chapter duration and pauses`() = runTest {
    val (vm, player) = viewModel(currentBookStoreId = book.id)

    vm.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsCompleted)

    val lastChapter = book.chapters.last()
    coVerifyOrder {
      player.setPosition(lastChapter.duration, lastChapter.id)
      player.pause()
    }
  }

  @Test
  fun `recategorizing a non-loaded book does not touch the player`() = runTest {
    val otherBookId = BookId("other")
    val (vm, player) = viewModel(currentBookStoreId = otherBookId)

    vm.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsNotStarted)

    coVerify(exactly = 0) { player.setPosition(any(), any()) }
    verify(exactly = 0) { player.pause() }
  }
}
