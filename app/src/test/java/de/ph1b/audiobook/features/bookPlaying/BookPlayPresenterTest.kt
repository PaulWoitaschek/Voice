package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.common.Optional
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.appComponent
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.Paused
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.Playing
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.Stopped
import de.ph1b.audiobook.playback.PlayerCommand
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.SleepTimer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import kotlinx.coroutines.channels.ConflatedBroadcastChannel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.nanoseconds
import kotlin.time.seconds

class BookPlayPresenterTest {

  private val bookPlayPresenter: BookPlayPresenter
  private val bookId = UUID.randomUUID()

  private val mockBookRepository = mockk<BookRepository>()
  private val mockPlayerController = mockk<PlayerController>(relaxUnitFun = true)
  private val mockPlayStateManager = mockk<PlayStateManager>()
  private val mockSleepTimer = mockk<SleepTimer>(relaxUnitFun = true)
  private val mockView = mockk<BookPlayMvp.View>(relaxUnitFun = true)

  init {
    appComponent = mockk(relaxed = true)
    bookPlayPresenter = BookPlayPresenter(bookId).apply {
      bookRepository = mockBookRepository
      playerController = mockPlayerController
      playStateManager = mockPlayStateManager
      sleepTimer = mockSleepTimer
      currentBookIdPref = mockk(relaxUnitFun = true)
    }
    every { mockBookRepository.booksStream() } returns Observable.empty()
    every { mockBookRepository.byId(any()) } returns Observable.just(Optional.Absent())
    every { mockPlayStateManager.playStateStream() } returns Observable.empty()
    every { mockSleepTimer.leftSleepTimeFlow } returns emptyFlow()
  }

  @Test
  fun sleepTimberShowsTime() {
    every { mockSleepTimer.leftSleepTimeFlow } returns flowOf(3.nanoseconds, 2.nanoseconds, 1.nanoseconds)
    bookPlayPresenter.attach(mockView)

    verifyOrder {
      mockView.showLeftSleepTime(3.nanoseconds)
      mockView.showLeftSleepTime(2.nanoseconds)
      mockView.showLeftSleepTime(1.nanoseconds)
    }
  }

  @Test
  fun sleepTimberStopsAfterDetach() {
    val sleepSand = ConflatedBroadcastChannel<Duration>()
    every { mockSleepTimer.leftSleepTimeFlow } returns sleepSand.asFlow()
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    sleepSand.offer(1.seconds)
    verify(exactly = 0) { mockView.showLeftSleepTime(any()) }
  }

  @Test
  fun playState() {
    every { mockPlayStateManager.playStateStream() }.returns(
      Observable.just(
        Playing,
        Stopped,
        Stopped,
        Playing,
        Paused,
        Stopped
      )
    )
    bookPlayPresenter.attach(mockView)
    verifyOrder {
      mockView.showPlaying(true)
      mockView.showPlaying(false)
      mockView.showPlaying(true)
      mockView.showPlaying(false)
    }
  }

  @Test
  fun playStateStopsAfterDetach() {
    val playStateStream = PublishSubject.create<PlayStateManager.PlayState>()
    every { mockPlayStateManager.playStateStream() } returns playStateStream
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    verify(exactly = 0) {
      mockView.showLeftSleepTime(any())
    }
  }

  @Test
  fun bookStreamStopsAfterDetach() {
    val bookStream = PublishSubject.create<List<Book>>()
    every { mockBookRepository.booksStream() } returns bookStream
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    val bookWithCorrectId = BookFactory.create(id = bookId, time = 0)
    bookStream.onNext(listOf(bookWithCorrectId))
    verify(exactly = 0) { mockView.render(any()) }
  }

  @Test
  fun absentBookFinishes() {
    val bookWithFalseId = BookFactory.create(id = UUID.randomUUID())
    bookRepoWillReturn(bookWithFalseId)
    bookPlayPresenter.attach(mockView)
    verify(exactly = 0) { mockView.render(any()) }
    verify(exactly = 1) { mockView.finish() }
  }

  @Test
  fun playPause() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.playPause()
    verify(exactly = 1) { mockPlayerController.execute(PlayerCommand.PlayPause) }
  }

  @Test
  fun rewind() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.rewind()
    verify(exactly = 1) { mockPlayerController.rewind() }
  }

  @Test
  fun fastForward() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.fastForward()
    verify(exactly = 1) { mockPlayerController.fastForward() }
  }

  @Test
  fun next() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.next()
    verify(exactly = 1) { mockPlayerController.execute(PlayerCommand.Next) }
  }

  @Test
  fun previous() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.previous()
    verify(exactly = 1) { mockPlayerController.execute(PlayerCommand.Previous) }
  }

  @Test
  fun seekToWithoutFileUsesBookFile() {
    val book = BookFactory.create(id = bookId)
    bookRepoWillReturn(book)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.seekTo(100, null)
    verify(exactly = 1) {
      mockPlayerController.execute(PlayerCommand.SetPosition(100, book.content.currentFile))
    }
  }

  private fun bookRepoWillReturn(book: Book) {
    every { mockBookRepository.byId(bookId) } answers {
      val id = invocation.args[0] as UUID
      Observable.just(Optional.of(book.takeIf { id == book.id }))
    }
    every { mockBookRepository.bookById(bookId) } answers {
      val id = invocation.args[0] as UUID
      book.takeIf { book.id == id }
    }
  }

  @Test
  fun seeToWithFile() {
    val book = BookFactory.create(id = bookId)
    every { mockBookRepository.bookById(bookId) } returns book
    bookPlayPresenter.attach(mockView)
    val lastFile = book.content.chapters.last().file
    bookPlayPresenter.seekTo(100, lastFile)
    verify(exactly = 1) {
      mockPlayerController.execute(PlayerCommand.SetPosition(100, lastFile))
    }
  }

  @Test
  fun toggleActiveSleepTimberCancels() {
    every { mockSleepTimer.sleepTimerActive() } returns true
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.toggleSleepTimer()
    verify(exactly = 1) { mockSleepTimer.setActive(false) }
    verify(exactly = 0) { mockView.openSleepTimeDialog() }
  }

  @Test
  fun toggleSleepTimberInInactiveStateOpensMenu() {
    every { mockSleepTimer.sleepTimerActive() } returns false
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.toggleSleepTimer()
    verify(exactly = 0) { mockSleepTimer.setActive(any()) }
  }
}
