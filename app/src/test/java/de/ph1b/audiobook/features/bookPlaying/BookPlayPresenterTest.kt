package de.ph1b.audiobook.features.bookPlaying

import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.common.Optional
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PAUSED
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PLAYING
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.STOPPED
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.SleepTimer
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import java.util.UUID

class BookPlayPresenterTest {

  private val bookPlayPresenter: BookPlayPresenter
  private val bookId = UUID.randomUUID()

  private val mockBookRepository = mockk<BookRepository>()
  private val mockPlayerController = mockk<PlayerController>(relaxUnitFun = true)
  private val mockPlayStateManager = mockk<PlayStateManager>()
  private val mockSleepTimer = mockk<SleepTimer>(relaxUnitFun = true)
  private val mockView = mockk<BookPlayMvp.View>(relaxUnitFun = true)

  init {
    App.component = mockk(relaxed = true)
    bookPlayPresenter = BookPlayPresenter(bookId).apply {
      bookRepository = mockBookRepository
      playerController = mockPlayerController
      playStateManager = mockPlayStateManager
      sleepTimer = mockSleepTimer
    }
    every { mockBookRepository.booksStream() } returns Observable.empty()
    every { mockBookRepository.byId(any()) } returns Observable.just(Optional.Absent())
    every { mockPlayStateManager.playStateStream() } returns Observable.empty()
    every { mockSleepTimer.leftSleepTimeInMsStream } returns Observable.empty()
  }

  @Test
  fun sleepTimberShowsTime() {
    every { mockSleepTimer.leftSleepTimeInMsStream } returns Observable.just(3, 2, 1)
    bookPlayPresenter.attach(mockView)

    verifyOrder {
      mockView.showLeftSleepTime(3)
      mockView.showLeftSleepTime(2)
      mockView.showLeftSleepTime(1)
    }
  }

  @Test
  fun sleepTimberStopsAfterDetach() {
    val sleepSand = PublishSubject.create<Long>()
    every { mockSleepTimer.leftSleepTimeInMsStream } returns sleepSand
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    sleepSand.onNext(1)
    verify(exactly = 0) { mockView.showLeftSleepTime(any()) }
  }

  @Test
  fun playState() {
    every { mockPlayStateManager.playStateStream() }.returns(
      Observable.just(
        PLAYING,
        STOPPED,
        STOPPED,
        PLAYING,
        PAUSED,
        STOPPED
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
    verify(exactly = 1) { mockPlayerController.playPause() }
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
    verify(exactly = 1) { mockPlayerController.next() }
  }

  @Test
  fun previous() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.previous()
    verify(exactly = 1) { mockPlayerController.previous() }
  }

  @Test
  fun seekToWithoutFileUsesBookFile() {
    val book = BookFactory.create(id = bookId)
    bookRepoWillReturn(book)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.seekTo(100, null)
    verify(exactly = 1) { mockPlayerController.changePosition(100, book.content.currentFile) }
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
    verify(exactly = 1) { mockPlayerController.changePosition(100, lastFile) }
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
