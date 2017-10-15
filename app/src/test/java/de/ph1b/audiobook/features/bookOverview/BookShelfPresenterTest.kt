package de.ph1b.audiobook.features.bookOverview

import com.nhaarman.mockito_kotlin.any
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.times
import com.nhaarman.mockito_kotlin.verify
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.MemoryPref
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PAUSED
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PLAYING
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.STOPPED
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit

class BookShelfPresenterTest {

  @Rule
  @JvmField
  val mockitoRule = MockitoJUnit.rule()!!

  private lateinit var presenter: BookShelfPresenter

  @Mock lateinit var repo: BookRepository
  @Mock lateinit var bookAdder: BookAdder
  @Mock lateinit var playStateManager: PlayStateManager
  @Mock lateinit var playerController: PlayerController
  @Mock lateinit var coverFromDiscCollector: CoverFromDiscCollector

  @Mock lateinit var view: BookShelfView

  private lateinit var currentBookIdPref: Pref<Long>

  @Before
  fun setUp() {
    currentBookIdPref = MemoryPref(-1L)
    presenter = BookShelfPresenter(
        repo = repo,
        bookAdder = bookAdder,
        playStateManager = playStateManager,
        playerController = playerController,
        coverFromDiscCollector = coverFromDiscCollector,
        currentBookIdPref = currentBookIdPref
    )

    given { repo.booksStream() }.willReturn(Observable.just(emptyList()))
    given { bookAdder.scannerActive }.willReturn(Observable.just(false))
    given { playStateManager.playStateStream() }
        .willReturn(Observable.just(PAUSED))
    given {
      coverFromDiscCollector.coverChanged()
    }.willReturn(Observable.never())
  }

  @Test
  fun scanForFiles() {
    presenter.attach(view)
    verify(bookAdder).scanForFiles()
  }

  @Test
  fun noFolderWarningShownWithoutBooks() {
    presenter.attach(view)
    verify(view).showNoFolderWarning()
  }

  @Test
  fun noFolderWarningNotShownWithBooks() {
    given { repo.booksStream() }
        .willReturn(Observable.just(listOf(BookFactory.create(id = 1))))
    presenter.attach(view)
    verify(view, never()).showNoFolderWarning()
  }

  @Test
  fun displayNewBooks() {
    val firstEmission = listOf(BookFactory.create(id = 1))
    val secondEmission = firstEmission + BookFactory.create(id = 2)
    given { repo.booksStream() }
        .willReturn(Observable.just(firstEmission, secondEmission))
    presenter.attach(view)
    inOrder(view) {
      verify(view).displayNewBooks(firstEmission)
      verify(view).displayNewBooks(secondEmission)
    }
  }

  @Test
  fun currentBook() {
    val firstBook = BookFactory.create(id = 1)
    given { repo.bookById(firstBook.id) }.willReturn(firstBook)
    val secondBook = BookFactory.create(id = 2)
    given { repo.bookById(secondBook.id) }.willReturn(secondBook)

    presenter.attach(view)
    currentBookIdPref.value = 0
    verify(view, never()).updateCurrentBook(any())
    currentBookIdPref.value = 1
    verify(view).updateCurrentBook(firstBook)
    currentBookIdPref.value = 2
    verify(view).updateCurrentBook(secondBook)
  }

  @Test
  fun scannerNotActiveNeverShowsLoading() {
    val emptyListObservable = Observable.just<List<Book>>(emptyList())
    val singleBookObservable = Observable.just(listOf(BookFactory.create()))
    given { repo.booksStream() }.willReturn(emptyListObservable, singleBookObservable)

    presenter.attach(view)

    verify(view).showLoading(false)
  }

  @Test
  fun loadingIfScannerActiveAndNoBooks() {
    given { repo.booksStream() }.willReturn(Observable.just(emptyList()))
    given { bookAdder.scannerActive }.willReturn(Observable.just(true))

    presenter.attach(view)
    verify(view).showLoading(true)
  }

  @Test
  fun notLoadingIfThereAreBooks() {
    given { repo.booksStream() }.willReturn(Observable.just(listOf(BookFactory.create())))
    given { bookAdder.scannerActive }.willReturn(Observable.just(true, false))

    presenter.attach(view)
    verify(view).showLoading(false)
  }

  @Test
  fun notLoadingIfScannerNotActiveAndNoBooks() {
    given { repo.booksStream() }.willReturn(Observable.just(emptyList()))
    given { bookAdder.scannerActive }.willReturn(Observable.just(false))

    presenter.attach(view)
    verify(view).showLoading(false)
  }

  @Test
  fun playState() {
    given {
      playStateManager.playStateStream()
    }.willReturn(Observable.just(PLAYING, PAUSED, STOPPED, PLAYING, STOPPED, PAUSED, PLAYING))
    presenter.attach(view)
    inOrder(view) {
      verify(view).showPlaying(true)
      verify(view).showPlaying(false)
      verify(view).showPlaying(true)
      verify(view).showPlaying(false)
      verify(view).showPlaying(true)
    }
  }

  @Test
  fun coverChanged() {
    given {
      coverFromDiscCollector.coverChanged()
    }.willReturn(Observable.just(1, 2, 3, 3, 2, 1))
    presenter.attach(view)
    inOrder(view) {
      verify(view).bookCoverChanged(1)
      verify(view).bookCoverChanged(2)
      verify(view, times(2)).bookCoverChanged(3)
      verify(view).bookCoverChanged(2)
      verify(view).bookCoverChanged(1)
    }
  }
}
