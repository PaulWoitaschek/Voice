package voice.features.bookOverview.editBookCategory

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
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

  private fun viewModel(playerController: PlayerController = mockk(relaxed = true)): Pair<EditBookCategoryViewModel, PlayerController> {
    val vm = EditBookCategoryViewModel(
      repo = repo,
      playerController = playerController,
    )
    return vm to playerController
  }

  @Test
  fun `MarkAsNotStarted on currently loaded book seeks player to 0 and pauses`() = runTest {
    val (vm, player) = viewModel()

    vm.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsNotStarted)

    coVerify { player.seekAndPauseIfCurrent(book.id, 0L, book.chapters.first().id) }
  }

  @Test
  fun `MarkAsCurrent on currently loaded book seeks player to position 1 and pauses`() = runTest {
    val (vm, player) = viewModel()

    vm.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsCurrent)

    coVerify { player.seekAndPauseIfCurrent(book.id, 1L, book.chapters.first().id) }
  }

  @Test
  fun `MarkAsCompleted on currently loaded book seeks player to last chapter duration and pauses`() = runTest {
    val (vm, player) = viewModel()

    vm.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsCompleted)

    val lastChapter = book.chapters.last()
    coVerify { player.seekAndPauseIfCurrent(book.id, lastChapter.duration, lastChapter.id) }
  }

  @Test
  fun `recategorizing always delegates to seekAndPauseIfCurrent regardless of which book is loaded`() = runTest {
    val (vm, player) = viewModel()

    vm.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsNotStarted)

    coVerify(exactly = 1) { player.seekAndPauseIfCurrent(book.id, any(), any()) }
    verify(exactly = 0) { player.pause() }
  }
}
