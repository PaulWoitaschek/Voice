package voice.features.bookOverview.editBookCategory

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.runs
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.test.runTest
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.features.bookOverview.MemoryDataStore
import voice.features.bookOverview.book
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.chapter
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration

class EditBookCategoryViewModelTest {

  private val lastChapter = chapter(duration = 7_000)
  private val book = book(chapters = listOf(chapter(duration = 5_000), lastChapter), time = 1_000)

  private val repo = mockk<BookRepository> {
    coEvery { get(book.id) } returns book
    coEvery { updateBook(book.id, any()) } just runs
  }

  private val playerController = mockk<PlayerController> {
    coEvery { livePlaybackState(book.id) } returns null
    every { setPosition(any(), any(), any()) } just runs
    every { pauseWithRewind(any()) } just runs
  }

  private val currentBookStore = MemoryDataStore<BookId?>(book.id)

  private val viewModel = EditBookCategoryViewModel(repo, playerController, currentBookStore)

  @Test
  fun `marking a book as completed moves it to the end`() = runTest {
    viewModel.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsCompleted)

    val update = slot<(BookContent) -> BookContent>()
    coVerify { repo.updateBook(book.id, capture(update)) }
    val updated = update.captured(book.content)
    assertEquals(lastChapter.id, updated.currentChapter)
    assertEquals(7_000, updated.positionInChapter)
  }

  @Test
  fun `marking the book the player holds moves the player too`() = runTest {
    playerHoldsBook()

    viewModel.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsCompleted)

    verify { playerController.setPosition(7_000, lastChapter.id, any()) }
    verify(exactly = 0) { playerController.pauseWithRewind(any()) }
  }

  @Test
  fun `marking the book the player holds as not started pauses it at the start`() = runTest {
    playerHoldsBook()

    viewModel.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsNotStarted)

    verify { playerController.setPosition(0, book.chapters.first().id, any()) }
    verify { playerController.pauseWithRewind(Duration.ZERO) }
  }

  @Test
  fun `marking a book the player doesn't hold leaves the player alone`() = runTest {
    viewModel.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsNotStarted)

    verify(exactly = 0) { playerController.setPosition(any(), any(), any()) }
  }

  @Test
  fun `the player is left alone when another book is the current one`() = runTest {
    playerHoldsBook()
    currentBookStore.updateData { BookId("other") }

    viewModel.onItemClick(book.id, BottomSheetItem.BookCategoryMarkAsCompleted)

    verify(exactly = 0) { playerController.setPosition(any(), any(), any()) }
  }

  private fun playerHoldsBook() {
    coEvery { playerController.livePlaybackState(book.id) } returns LivePlaybackState(
      bookId = book.id,
      chapterId = book.content.currentChapter,
      positionMs = 1_000,
      isPlaying = true,
      playbackSpeed = 1F,
    )
  }
}
