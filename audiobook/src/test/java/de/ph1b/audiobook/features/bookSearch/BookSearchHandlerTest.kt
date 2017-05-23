package de.ph1b.audiobook.features.bookSearch

import android.os.Build
import android.provider.MediaStore
import android.support.annotation.RequiresApi
import com.f2prateek.rx.preferences.Preference
import de.ph1b.audiobook.BookMocker
import de.ph1b.audiobook.given
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayerController
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations


/**
 * A test case to easily test the voice search functionality for Android auto (and OK google commands)
 *
 *  @author Matthias Kutscheid
 *  @author Paul Woitaschek
 */
class BookSearchHandlerTest {

  lateinit var searchHandler: BookSearchHandler

  @Mock lateinit var repo: BookRepository
  @Mock lateinit var prefs: PrefsManager
  @Mock lateinit var player: PlayerController
  @Mock lateinit var currentBookIdPref: Preference<Long>

  private val book = BookMocker.mock().copy(id = 5)

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
    verify(currentBookIdPref).set(book.id)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testUnstructuredSearchByArtist() {
    val bookSearch = BookSearch(query = book.author)
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(book.id)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testUnstructuredSearchByChapter() {
    val bookSearch = BookSearch(query = book.chapters.first().name)
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(book.id)
    verifyNoMoreInteractions(currentBookIdPref)
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
    verify(currentBookIdPref).set(book.id)
    verifyNoMoreInteractions(currentBookIdPref)
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
    verify(currentBookIdPref).set(book.id)
    verifyNoMoreInteractions(currentBookIdPref)
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
    verify(currentBookIdPref).set(book.id)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusPlaylistWithAlbumInsteadOfPlaylist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = book.author,
        playList = book.name
    )
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(book.id)
    verifyNoMoreInteractions(currentBookIdPref)
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
