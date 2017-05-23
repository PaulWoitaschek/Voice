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

  private val bookId = 5L

  @Before
  fun setUp() {
    val dummy = BookMocker.mock().copy(id = bookId)
    MockitoAnnotations.initMocks(this)

    given { repo.activeBooks }.thenReturn(listOf(dummy))
    given { prefs.currentBookId }.thenReturn(currentBookIdPref)

    searchHandler = BookSearchHandler(repo, prefs, player)
  }

  @Test
  fun testUnstructuredSearchByBook() {
    val bookSearch = BookSearch(query = "TestBook")
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(bookId)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testUnstructuredSearchByArtist() {
    val bookSearch = BookSearch(query = "TestAuthor")
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(bookId)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testUnstructuredSearchByChapter() {
    val bookSearch = BookSearch(query = "first")
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(bookId)
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
        artist = "TestAuthor"
    )
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(bookId)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusArtistNoSuccess() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
        artist = "TestAuthor1"
    )
    searchHandler.handle(bookSearch)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusAlbum() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
        artist = "TestAuthor",
        album = "TestBook"
    )
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(bookId)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusAlbumNoSuccessBecauseOfArtist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
        artist = "TestAuthor1",
        album = "TestBook"
    )
    searchHandler.handle(bookSearch)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusAlbumNoSuccessBecauseOfAlbum() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
        artist = "TestAuthor",
        album = "TestBook1"
    )
    searchHandler.handle(bookSearch)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = "TestAuthor",
        playList = "TestBook",
        album = "TestBook"
    )
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(bookId)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @Test
  fun testMediaFocusPlaylistWithAlbumInsteadOfPlaylist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = "TestAuthor",
        playList = "TestBook"
    )
    searchHandler.handle(bookSearch)
    verify(currentBookIdPref).set(bookId)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylistNoSuccessBecauseOfArtist() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = "TestAuthor1",
        playList = "TestBook"
    )
    searchHandler.handle(bookSearch)
    verifyNoMoreInteractions(currentBookIdPref)
  }

  @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
  @Test
  fun testMediaFocusPlaylistNoSuccessBecauseOfAlbum() {
    val bookSearch = BookSearch(
        mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
        artist = "TestAuthor",
        playList = "TestBook1"
    )
    searchHandler.handle(bookSearch)
    verifyNoMoreInteractions(currentBookIdPref)
  }
}
