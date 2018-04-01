package de.ph1b.audiobook.features.bookPlaying

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.mock
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.given
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PAUSED
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PLAYING
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.STOPPED
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.SleepTimer
import io.reactivex.Observable
import io.reactivex.subjects.PublishSubject
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit

class BookPlayPresenterTest {

  @Rule
  @JvmField
  val mockitoRule = MockitoJUnit.rule()!!

  private lateinit var bookPlayPresenter: BookPlayPresenter
  private val bookId = 5L

  @Mock
  lateinit var mockBookRepository: BookRepository
  @Mock
  lateinit var mockPlayerController: PlayerController
  @Mock
  lateinit var mockPlayStateManager: PlayStateManager
  @Mock
  lateinit var mockSleepTimer: SleepTimer
  @Mock
  lateinit var mockView: BookPlayMvp.View

  @Before
  fun setUp() {
    App.component = mock()
    bookPlayPresenter = BookPlayPresenter(bookId).apply {
      bookRepository = mockBookRepository
      playerController = mockPlayerController
      playStateManager = mockPlayStateManager
      sleepTimer = mockSleepTimer
    }

    given { mockBookRepository.booksStream() }.thenReturn(Observable.empty())
    given { mockPlayStateManager.playStateStream() }.thenReturn(Observable.empty())
    given { mockSleepTimer.leftSleepTimeInMs }.thenReturn(Observable.empty())
  }

  @Test
  fun sleepTimberShowsTime() {
    given { mockSleepTimer.leftSleepTimeInMs }.thenReturn(Observable.just(3, 2, 1))
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
    given { mockSleepTimer.leftSleepTimeInMs }.thenReturn(sleepSand)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    sleepSand.onNext(1)
    verify(mockView, never()).showLeftSleepTime(any())
  }

  @Test
  fun playState() {
    given { mockPlayStateManager.playStateStream() }.thenReturn(
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
    given { mockPlayStateManager.playStateStream() }.thenReturn(playStateStream)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    verify(mockView, never()).showLeftSleepTime(any())
  }

  @Test
  fun bookStream() {
    val bookWithCorrectId = BookFactory.create(id = bookId, time = 0)
    val bookWithCorrectIdAndChangedTime =
      bookWithCorrectId.updateContent { copy(positionInChapter = 123) }
    val bookWithFalseId = BookFactory.create(id = 50)
    val firstEmission = listOf(bookWithCorrectId, bookWithFalseId)
    val secondEmission = listOf(bookWithCorrectIdAndChangedTime, bookWithFalseId)
    given { mockBookRepository.booksStream() }.thenReturn(
      Observable.just(
        firstEmission,
        secondEmission
      )
    )
    bookPlayPresenter.attach(mockView)
    inOrder(mockView) {
      verify(mockView).render(bookWithCorrectId)
      verify(mockView).render(bookWithCorrectIdAndChangedTime)
    }
  }

  @Test
  fun bookStreamStopsAfterDetach() {
    val bookStream = PublishSubject.create<List<Book>>()
    given { mockBookRepository.booksStream() }.thenReturn(bookStream)
    bookPlayPresenter.attach(mockView)
    bookPlayPresenter.detach()
    val bookWithCorrectId = BookFactory.create(id = bookId, time = 0)
    bookStream.onNext(listOf(bookWithCorrectId))
    verify(mockView, never()).render(any())
  }

  @Test
  fun absentBookFinishes() {
    val bookWithFalseId = BookFactory.create(id = 50)
    given { mockBookRepository.booksStream() }.thenReturn(Observable.just(listOf(bookWithFalseId)))
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
    bookPlayPresenter.attach(mockView)
    val book = BookFactory.create(id = bookId)
    given { mockBookRepository.bookById(bookId) }.thenReturn(book)
    bookPlayPresenter.seekTo(100, null)
    verify(mockPlayerController).changePosition(100, book.content.currentFile)
  }

  @Test
  fun seeToWithFile() {
    bookPlayPresenter.attach(mockView)
    val book = BookFactory.create(id = bookId)
    given { mockBookRepository.bookById(bookId) }.thenReturn(book)
    val lastFile = book.content.chapters.last().file
    bookPlayPresenter.seekTo(100, lastFile)
    verify(mockPlayerController).changePosition(100, lastFile)
  }

  @Test
  fun toggleActiveSleepTimberCancels() {
    bookPlayPresenter.attach(mockView)
    given { mockSleepTimer.sleepTimerActive() }.thenReturn(true)
    bookPlayPresenter.toggleSleepTimer()
    verify(mockSleepTimer).setActive(false)
    verify(mockView, never()).openSleepTimeDialog()
  }

  @Test
  fun toggleSleepTimberInInactiveStateOpensMenu() {
    bookPlayPresenter.attach(mockView)
    given { mockSleepTimer.sleepTimerActive() }.thenReturn(false)
    bookPlayPresenter.toggleSleepTimer()
    verify(mockSleepTimer, never()).setActive(any())
  }
}
