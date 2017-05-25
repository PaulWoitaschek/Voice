package de.ph1b.audiobook.features.bookSearch

import android.annotation.SuppressLint
import android.os.Build
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import com.f2prateek.rx.preferences.Preference
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.given
import de.ph1b.audiobook.misc.emptySparseArray
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayerController
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.*
import org.mockito.MockitoAnnotations
import java.io.File


/**
 * A test case to easily test the voice search functionality for Android auto (and OK google commands)
 *
 *  @author Matthias Kutscheid
 *  @author Paul Woitaschek
 */
@SuppressLint("SdCardPath")
class BookSearchHandlerTest {

  lateinit var searchHandler: BookSearchHandler

  @Mock lateinit var repo: BookRepository
  @Mock lateinit var prefs: PrefsManager
  @Mock lateinit var player: PlayerController

  @Mock lateinit var currentBookIdPref: Preference<Long>

  private val anotherBookChapter1 = Chapter(File("/sdcard/AnotherBook/chapter1.mp3"), "anotherBookChapter1", 5000, 0, emptySparseArray())
  private val anotherBookChapter2 = Chapter(File("/sdcard/AnotherBook/chapter2.mp3"), "anotherBookChapter2", 10000, 0, emptySparseArray())
  private val anotherBook = Book(2, Book.Type.SINGLE_FOLDER, "AnotherBookAuthor", anotherBookChapter1.file, 3000, "AnotherBook", listOf(anotherBookChapter1, anotherBookChapter2), 1F, "/sdcard/AnotherBook")

  private val bookToFindChapter1 = Chapter(File("/sdcard/Book1/chapter1.mp3"), "bookToFindChapter1", 5000, 0, emptySparseArray())
  private val bookToFindChapter2 = Chapter(File("/sdcard/Book1/chapter2.mp3"), "bookToFindChapter2", 10000, 0, emptySparseArray())
  private val bookToFind = Book(1, Book.Type.SINGLE_FOLDER, "Book1Author", bookToFindChapter2.file, 3000, "Book1", listOf(bookToFindChapter1, bookToFindChapter2), 1F, "/sdcard/Book1")

  @Before fun setUp() {
    MockitoAnnotations.initMocks(this)

    given { repo.activeBooks }.thenReturn(listOf(anotherBook, bookToFind))
    given { prefs.currentBookId }.thenReturn(currentBookIdPref)

    searchHandler = BookSearchHandler(repo, prefs, player)
  }

  @Test fun testUnstructuredSearchByBook() {
    val bookSearch = BookSearch(query = bookToFind.name)
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(bookToFind.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test fun testUnstructuredSearchByArtist() {
    val bookSearch = BookSearch(query = bookToFind.author)
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(bookToFind.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test fun testUnstructuredSearchByChapter() {
    val bookSearch = BookSearch(query = bookToFindChapter1.name)
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(bookToFind.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test fun testMediaFocusAnyNoneFoundButPlayed() {
    given { currentBookIdPref.get() }.thenReturn(-1)

    val bookSearch = BookSearch(mediaFocus = "vnd.android.cursor.item/*")
    searchHandler.handle(bookSearch)

    verify(currentBookIdPref).set(anotherBook.id)
    verify(player).play()
  }

  @Test fun testMediaFocusArtist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
        artist = bookToFind.author
    )
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(bookToFind.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test fun testMediaFocusArtistInTitleNoArtistInBook() {
    val bookToFind = this.bookToFind.copy(author = null, name = "The book of Tim")
    given { repo.activeBooks }.thenReturn(listOf(bookToFind))

    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
        query = "Tim",
        artist = "Tim"
    )
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(bookToFind.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test fun testMediaFocusAlbum() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
        artist = bookToFind.author,
        album = bookToFind.name
    )
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(bookToFind.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = bookToFind.author,
        playList = bookToFind.name,
        album = bookToFind.name
    )
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(bookToFind.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }
}
