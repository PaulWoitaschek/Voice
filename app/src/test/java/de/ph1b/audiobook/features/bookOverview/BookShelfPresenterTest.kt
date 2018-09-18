/*
package de.ph1b.audiobook.features.bookOverview

import com.google.common.truth.Truth.assertThat
import com.nhaarman.mockito_kotlin.argThat
import com.nhaarman.mockito_kotlin.argumentCaptor
import com.nhaarman.mockito_kotlin.atLeastOnce
import com.nhaarman.mockito_kotlin.given
import com.nhaarman.mockito_kotlin.inOrder
import com.nhaarman.mockito_kotlin.never
import com.nhaarman.mockito_kotlin.verify
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.MemoryPref
import de.ph1b.audiobook.RxMainThreadTrampolineRule
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.BookAdder
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PAUSED
import de.ph1b.audiobook.playback.PlayStateManager.PlayState.PLAYING
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.uitools.CoverFromDiscCollector
import io.reactivex.Observable
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.junit.MockitoJUnit
import java.util.UUID fixme!!

class BookShelfPresenterTest {

  @Rule
  @JvmField
  val mockitoRule = MockitoJUnit.rule()!!

  @Rule
  @JvmField
  val trampolineRule = RxMainThreadTrampolineRule()

  private lateinit var presenter: BookOverviewViewModel

  @Mock
  lateinit var repo: BookRepository
  @Mock
  lateinit var bookAdder: BookAdder
  @Mock
  lateinit var playStateManager: PlayStateManager
  @Mock
  lateinit var playerController: PlayerController
  @Mock
  lateinit var coverFromDiscCollector: CoverFromDiscCollector

  @Mock
  lateinit var view: BookShelfView

  private lateinit var currentBookIdPref: Pref<UUID>

  @Before
  fun setUp() {
    currentBookIdPref = MemoryPref(UUID.randomUUID())
    presenter = BookOverviewViewModel(
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
    verify(view).render(BookOverviewState.NoFolderSet)
  }

  @Test
  fun noFolderWarningNotShownWithBooks() {
    given { repo.booksStream() }
      .willReturn(Observable.just(listOf(BookFactory.create())))
    presenter.attach(view)
    verify(view, never()).render(BookOverviewState.NoFolderSet)
  }

  @Test
  fun displayNewBooks() {
    val firstEmission = listOf(BookFactory.create())
    val secondEmission = firstEmission + BookFactory.create()
    given { repo.booksStream() }
      .willReturn(Observable.just(firstEmission, secondEmission))
    presenter.attach(view)
    val captor = argumentCaptor<BookOverviewState>()
    verify(view).render(captor.capture())
    val lastValue = captor.lastValue
    assertThat(lastValue is BookOverviewState.Content && lastValue.books == secondEmission)
  }

  @Test
  fun currentBook() {
    val firstBook = BookFactory.create()
    val secondBook = BookFactory.create()
    given { repo.booksStream() }
      .willReturn(Observable.just(listOf(firstBook, secondBook)))

    presenter.attach(view)
    currentBookIdPref.value = UUID.randomUUID()
    currentBookIdPref.value = firstBook.id
    currentBookIdPref.value = secondBook.id

    inOrder(view) {
      verify(view, atLeastOnce()).render(
        argThat { this is BookOverviewState.Content && currentBook == null }
      )
      verify(view).render(
        argThat { this is BookOverviewState.Content && currentBook == firstBook }
      )
      verify(view).render(
        argThat { this is BookOverviewState.Content && currentBook == secondBook }
      )
    }
  }

  @Test
  fun scannerNotActiveNeverShowsLoading() {
    val emptyListObservable = Observable.just<List<Book>>(emptyList())
    val singleBookObservable = Observable.just(listOf(BookFactory.create()))
    given { repo.booksStream() }.willReturn(emptyListObservable, singleBookObservable)

    presenter.attach(view)

    verify(view, never()).render(
      argThat { this is BookOverviewState.Loading }
    )
  }

  @Test
  fun loadingIfScannerActiveAndNoBooks() {
    given { repo.booksStream() }.willReturn(Observable.just(emptyList()))
    given { bookAdder.scannerActive }.willReturn(Observable.just(true))

    presenter.attach(view)
    verify(view).render(BookOverviewState.Loading)
  }

  @Test
  fun notLoadingIfThereAreBooks() {
    given { repo.booksStream() }.willReturn(Observable.just(listOf(BookFactory.create())))
    given { bookAdder.scannerActive }.willReturn(Observable.just(true, false))

    presenter.attach(view)
    verify(view, never()).render(BookOverviewState.Loading)
  }

  @Test
  fun notLoadingIfScannerNotActiveAndNoBooks() {
    given { repo.booksStream() }.willReturn(Observable.just(emptyList()))
    given { bookAdder.scannerActive }.willReturn(Observable.just(false))

    presenter.attach(view)
    verify(view, never()).render(BookOverviewState.Loading)
  }

  @Test
  fun playStatePlaying() {
    given { repo.booksStream() }.willReturn(Observable.just(listOf(BookFactory.create())))
    given {
      playStateManager.playStateStream()
    }.willReturn(Observable.just(PLAYING))
    presenter.attach(view)

    verify(view).render(argThat { this is BookOverviewState.Content && playing })
  }

  @Test
  fun playStatePaused() {
    given { repo.booksStream() }.willReturn(Observable.just(listOf(BookFactory.create())))
    given {
      playStateManager.playStateStream()
    }.willReturn(Observable.just(PAUSED))
    presenter.attach(view)

    verify(view).render(argThat { this is BookOverviewState.Content && !playing })
  }


  @Test
  fun playStateStopped() {
    given { repo.booksStream() }.willReturn(Observable.just(listOf(BookFactory.create())))
    given {
      playStateManager.playStateStream()
    }.willReturn(Observable.just(PAUSED))
    presenter.attach(view)

    verify(view).render(argThat { this is BookOverviewState.Content && !playing })
  }


  @Test
  fun coverChanged() {
    val firstId = UUID.randomUUID()
    val secondId = UUID.randomUUID()
    given {
      coverFromDiscCollector.coverChanged()
    }.willReturn(Observable.just(firstId, secondId))
    presenter.attach(view)
    inOrder(view) {
      verify(view).bookCoverChanged(firstId)
      verify(view).bookCoverChanged(secondId)
    }
  }
}
*/
