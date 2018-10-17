package de.ph1b.audiobook.features.bookPlaying

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.doAnswer
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import com.nhaarman.mockito_kotlin.whenever
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
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Test
import java.util.UUID

class BookPlayPresenterTest {

  private val bookPlayPresenter: BookPlayPresenter
  private val bookId = UUID.randomUUID()

  private val mockBookRepository: BookRepository = mock()
  private val mockPlayerController: PlayerController = mock()
  private val mockPlayStateManager: PlayStateManager = mock()
  private val mockSleepTimer: SleepTimer = mock()
  private val mockView: BookPlayMvp.View = mock()

  init {
    App.component = mock()
    bookPlayPresenter = BookPlayPresenter(bookId).apply {
      bookRepository = mockBookRepository
      playerController = mockPlayerController
      playStateManager = mockPlayStateManager
      sleepTimer = mockSleepTimer
    }

    whenever(mockBookRepository.booksStream()).thenReturn(Observable.empty())
    whenever(mockBookRepository.byId(any())).thenReturn(Observable.just(Optional.Absent()))
    whenever(mockPlayStateManager.playStateStream()).thenReturn(Observable.empty())
    whenever(mockSleepTimer.leftSleepTimeInMs).thenReturn(Observable.empty())
  }

  @Test
  fun sleepTimberShowsTime() {
    whenever(mockSleepTimer.leftSleepTimeInMs).thenReturn(Observable.just(3, 2, 1))
    bookPlayPresenter.attach(mockView)

    inOrder(mockView).apply {
      verify(mockView).showLeftSleepTime(3)
      verify(mockView).showLeftSleepTime(2)
      verify(mockView).showLeftSleepTime(1)
    }
  }

  @Test
  fun sleepTimberStopsAfterDetach() {
    val sleepSand = PublishSubject.create<Int>()
    whenever(mockSleepTimer.leftSleepTimeInMs).thenReturn(sleepSand)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    sleepSand.onNext(1)
    verify(mockView, never()).showLeftSleepTime(any())
  }

  @Test
  fun playState() {
    whenever(mockPlayStateManager.playStateStream()).thenReturn(
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
    inOrder(mockView) {
      verify(mockView).showPlaying(true)
      verify(mockView).showPlaying(false)
      verify(mockView).showPlaying(true)
      verify(mockView).showPlaying(false)
    }
  }

  @Test
  fun playStateStopsAfterDetach() {
    val playStateStream = PublishSubject.create<PlayStateManager.PlayState>()
    whenever(mockPlayStateManager.playStateStream()).thenReturn(playStateStream)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    verify(mockView, never()).showLeftSleepTime(any())
  }

  @Test
  fun bookStreamStopsAfterDetach() {
    val bookStream = PublishSubject.create<List<Book>>()
    whenever(mockBookRepository.booksStream()).thenReturn(bookStream)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    val bookWithCorrectId = BookFactory.create(id = bookId, time = 0)
    bookStream.onNext(listOf(bookWithCorrectId))
    verify(mockView, never()).render(any())
  }

  @Test
  fun absentBookFinishes() {
    val bookWithFalseId = BookFactory.create(id = UUID.randomUUID())
    bookRepoWillReturn(bookWithFalseId)
    bookPlayPresenter.attach(mockView)
    verify(mockView, never()).render(any())
    verify(mockView).finish()
  }

  @Test
  fun playPause() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.playPause()
    verify(mockPlayerController).playPause()
  }

  @Test
  fun rewind() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.rewind()
    verify(mockPlayerController).rewind()
  }

  @Test
  fun fastForward() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.fastForward()
    verify(mockPlayerController).fastForward()
  }

  @Test
  fun next() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.next()
    verify(mockPlayerController).next()
  }

  @Test
  fun previous() {
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.previous()
    verify(mockPlayerController).previous()
  }

  @Test
  fun seekToWithoutFileUsesBookFile() {
    val book = BookFactory.create(id = bookId)
    bookRepoWillReturn(book)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.seekTo(100, null)
    verify(mockPlayerController).changePosition(100, book.content.currentFile)
  }

  private fun bookRepoWillReturn(book: Book) {
    whenever(mockBookRepository.byId(bookId)).doAnswer { invocation ->
      val id = invocation.getArgument<UUID>(0)
      Observable.just(Optional.of(book.takeIf { id == book.id }))
    }
    whenever(mockBookRepository.bookById(bookId)).doAnswer { invocation ->
      val id = invocation.getArgument<UUID>(0)
      book.takeIf { book.id == id }
    }
  }

  @Test
  fun seeToWithFile() {
    val book = BookFactory.create(id = bookId)
    whenever(mockBookRepository.bookById(bookId)).thenReturn(book)
    bookPlayPresenter.attach(mockView)
    val lastFile = book.content.chapters.last().file
    bookPlayPresenter.seekTo(100, lastFile)
    verify(mockPlayerController).changePosition(100, lastFile)
  }

  @Test
  fun toggleActiveSleepTimberCancels() {
    whenever(mockSleepTimer.sleepTimerActive()).thenReturn(true)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.toggleSleepTimer()
    verify(mockSleepTimer).setActive(false)
    verify(mockView, never()).openSleepTimeDialog()
  }

  @Test
  fun toggleSleepTimberInInactiveStateOpensMenu() {
    whenever(mockSleepTimer.sleepTimerActive()).thenReturn(false)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.toggleSleepTimer()
    verify(mockSleepTimer, never()).setActive(any())
  }
}
