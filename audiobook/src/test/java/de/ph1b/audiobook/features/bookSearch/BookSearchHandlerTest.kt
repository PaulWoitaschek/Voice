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
import org.mockito.Mockito.inOrder
import org.mockito.Mockito.verifyNoMoreInteractions
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

  private val chapter1 = Chapter(File("/sdcard/Book1/chapter1.mp3"), "chapter1", 5000, 0, emptySparseArray())
  private val chapter2 = Chapter(File("/sdcard/Book1/chapter2.mp3"), "chapter2", 10000, 0, emptySparseArray())
  private val book = Book(1, Book.Type.SINGLE_FOLDER, "Book1Author", chapter2.file, 3000, "Book1", listOf(chapter1, chapter2), 1F, "/sdcard/Book1")

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)

    given { repo.activeBooks }.thenReturn(listOf(book))
    given { prefs.currentBookId }.thenReturn(currentBookIdPref)

    searchHandler = BookSearchHandler(repo, prefs, player)
  }

  @Test
  fun testUnstructuredSearchByBook() {
    val bookSearch = BookSearch(query = book.name)
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(book.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test
  fun testUnstructuredSearchByArtist() {
    val bookSearch = BookSearch(query = book.author)
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(book.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test
  fun testUnstructuredSearchByChapter() {
    val bookSearch = BookSearch(query = chapter1.name)
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(book.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test
  fun testMediaFocusAnyNonePlayed() {
    val bookSearch = BookSearch(mediaFocus = "vnd.android.cursor.item/*")
    searchHandler.handle(bookSearch)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusArtist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
        artist = book.author
    )
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(book.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test
  fun testMediaFocusArtistNoSuccess() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
        artist = "UnknownAuthor"
    )
    searchHandler.handle(bookSearch)

    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusAlbum() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
        artist = book.author,
        album = book.name
    )
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(book.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test
  fun testMediaFocusAlbumNoSuccessBecauseOfArtist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
        artist = "UnknownAuthor",
        album = book.name
    )
    searchHandler.handle(bookSearch)

    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusAlbumNoSuccessBecauseOfAlbum() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
        artist = book.author,
        album = "UnknownBook"
    )
    searchHandler.handle(bookSearch)

    verifyNoMoreInteractions(currentBookIdPref)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = book.author,
        playList = book.name,
        album = book.name
    )
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(book.id)
      verify(player).play()
    }

    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @Test
  fun testMediaFocusPlaylistWithAlbumInsteadOfPlaylist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = book.author,
        playList = book.name
    )
    searchHandler.handle(bookSearch)

    inOrder(currentBookIdPref, player).apply {
      verify(currentBookIdPref).set(book.id)
      verify(player).play()
    }
    verifyNoMoreInteractions(currentBookIdPref)
    verifyNoMoreInteractions(player)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylistNoSuccessBecauseOfArtist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = "UnknownAuthor",
        playList = book.name
    )
    searchHandler.handle(bookSearch)

    verifyNoMoreInteractions(currentBookIdPref)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylistNoSuccessBecauseOfAlbum() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = book.author,
        playList = "UnknownBook"
    )
    searchHandler.handle(bookSearch)

    verifyNoMoreInteractions(currentBookIdPref)
  }
}
