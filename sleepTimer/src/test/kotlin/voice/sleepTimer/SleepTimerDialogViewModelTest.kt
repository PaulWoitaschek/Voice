package voice.sleepTimer

import app.cash.turbine.test
import de.paulwoitaschek.flowpref.inmemory.InMemoryPref
import io.kotest.matchers.ints.shouldBeExactly
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.common.BookId
import voice.common.DispatcherProvider
import voice.data.Book
import voice.data.BookContent
import voice.data.Chapter
import voice.data.ChapterId
import voice.data.repo.BookRepository
import voice.data.repo.BookmarkRepo
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

class SleepTimerDialogViewModelTest {

  private val book = book()
  private val sleepTimePref = InMemoryPref(42)
  private val scope = TestScope()
  private val sleepTimer = mockk<SleepTimer>().also {
    every { it.setActive(any()) } just Runs
  }
  private val bookRepo = mockk<BookRepository>().also {
    coEvery { it.get(book.id) } returns book
  }
  private val bookmarkRepo = mockk<BookmarkRepo>().also {
    coEvery {
      it.addBookmarkAtBookPosition(
        book = book,
        title = null,
        setBySleepTimer = true,
      )
    } returns mockk()
  }
  private val viewModel = SleepTimerDialogViewModel(
    bookmarkRepo = bookmarkRepo,
    sleepTimer = sleepTimer,
    bookRepo = bookRepo,
    sleepTimePref = sleepTimePref,
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun `sleep time pref used as default value`() = test {
    viewModel.viewState().test {
      awaitItem().selectedMinutes shouldBeExactly 42
    }
  }

  @Test
  fun `sleep time pref updated when adding`() = test {
    viewModel.onNumberClicked(1)
    sleepTimePref.value shouldBe 42
    viewModel.onConfirmButtonClicked(book.id)
    sleepTimePref.value shouldBe 421
  }

  @Test
  fun `changing values works`() = test {
    viewModel.viewState().test {
      suspend fun expect(selectedMinutes: Int, showFab: Boolean = true) {
        awaitItem() shouldBe SleepTimerDialogViewState(
          selectedMinutes = selectedMinutes,
          showFab = showFab,
        )
      }
      expect(42)
      viewModel.onNumberClicked(4)
      expect(424)
      viewModel.onNumberClicked(4)
      expectNoEvents()
      viewModel.onNumberDeleteClicked()
      expect(42)
      viewModel.onNumberDeleteClicked()
      expect(4)
      viewModel.onNumberDeleteClicked()
      expect(0, showFab = false)
      viewModel.onNumberClicked(1)
      expect(1)
      viewModel.onNumberClicked(2)
      expect(12)
      viewModel.onNumberDeleteLongClicked()
      expect(0, showFab = false)
    }
  }

  @Test
  fun `bookmark is added`() = test {
    viewModel.onConfirmButtonClicked(book.id)
    runCurrent()
    coVerify(exactly = 1) {
      sleepTimer.setActive(true)
      bookmarkRepo.addBookmarkAtBookPosition(book, title = null, setBySleepTimer = true)
    }
  }

  private fun test(testBody: suspend TestScope.() -> Unit) {
    scope.runTest(testBody = testBody)
  }
}

private fun book(
  name: String = "TestBook",
  lastPlayedAtMillis: Long = 0L,
  addedAtMillis: Long = 0L,
): Book {
  val chapters = listOf(
    chapter(),
    chapter(),
  )
  return Book(
    content = BookContent(
      author = UUID.randomUUID().toString(),
      name = name,
      positionInChapter = 42,
      playbackSpeed = 1F,
      addedAt = Instant.ofEpochMilli(addedAtMillis),
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = chapters.first().id,
      isActive = true,
      lastPlayedAt = Instant.ofEpochMilli(lastPlayedAtMillis),
      skipSilence = false,
      id = BookId(UUID.randomUUID().toString()),
      gain = 0F,
    ),
    chapters = chapters,
  )
}

private fun chapter(): Chapter {
  return Chapter(
    id = ChapterId("http://${UUID.randomUUID()}"),
    duration = 5.minutes.inWholeMilliseconds,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    name = "name",
  )
}
