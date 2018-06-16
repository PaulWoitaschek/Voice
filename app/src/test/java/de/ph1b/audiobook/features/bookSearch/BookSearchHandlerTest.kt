package de.ph1b.audiobook.features.bookSearch

import android.annotation.SuppressLint
import android.provider.MediaStore
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.MemoryPref
import de.ph1b.audiobook.common.sparseArray.emptySparseArray
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.BookContent
import de.ph1b.audiobook.data.BookMetaData
import de.ph1b.audiobook.data.BookSettings
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.given
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayerController
import org.junit.Before
import org.junit.Test
import org.mockito.Mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoMoreInteractions
import org.mockito.MockitoAnnotations
import java.io.File
import java.util.UUID

/**
 * A test case to easily test the voice search functionality for Android auto (and OK google commands)
 */
@SuppressLint("SdCardPath")
class BookSearchHandlerTest {

  lateinit var searchHandler: BookSearchHandler

  @Mock
  lateinit var repo: BookRepository
  @Mock
  lateinit var player: PlayerController

  private lateinit var currentBookIdPref: Pref<UUID>

  private val anotherBookChapter1 = Chapter(
    File("/sdcard/AnotherBook/chapter1.mp3"),
    "anotherBookChapter1",
    5000,
    0,
    emptySparseArray(),
    bookId = UUID.randomUUID()
  )
  private val anotherBookChapter2 = Chapter(
    File("/sdcard/AnotherBook/chapter2.mp3"),
    "anotherBookChapter2",
    10000,
    0,
    emptySparseArray(),
    bookId = UUID.randomUUID()
  )

  private val anotherBook = UUID.randomUUID().let { id ->
    Book(
      id = id,
      metaData = BookMetaData(
        id = id,
        author = "AnotherBookAuthor",
        type = Book.Type.SINGLE_FOLDER,
        name = "AnotherBook",
        root = "/sdcard/AnotherBook"
      ),
      content = BookContent(
        id = id,
        settings = BookSettings(
          id = id,
          currentFile = anotherBookChapter1.file,
          positionInChapter = 3000,
          playbackSpeed = 1F,
          loudnessGain = 0
        ),
        chapters = listOf(
          anotherBookChapter1.copy(bookId = id),
          anotherBookChapter2.copy(bookId = id)
        )
      )
    )
  }

  private val bookToFindChapter1 =
    Chapter(
      File("/sdcard/Book1/chapter1.mp3"),
      "bookToFindChapter1",
      5000,
      0,
      emptySparseArray(),
      UUID.randomUUID()
    )
  private val bookToFindChapter2 =
    Chapter(
      File("/sdcard/Book1/chapter2.mp3"),
      "bookToFindChapter2",
      10000,
      0,
      emptySparseArray(),
      bookId = UUID.randomUUID()
    )
  private val bookToFind = UUID.randomUUID().let { id ->
    Book(
      metaData = BookMetaData(
        id = id,
        type = Book.Type.SINGLE_FOLDER,
        author = "Book1Author",
        name = "Book1",
        root = "/sdcard/Book1"
      ),
      id = id,
      content = BookContent(
        settings = BookSettings(
          id = id,
          currentFile = bookToFindChapter2.file,
          positionInChapter = 3000,
          playbackSpeed = 1F,
          loudnessGain = 0
        ),
        id = id,
        chapters = listOf(
          bookToFindChapter1.copy(bookId = id),
          bookToFindChapter2.copy(bookId = id)
        )
      )
    )
  }

  @Before
  fun setUp() {
    MockitoAnnotations.initMocks(this)

    given { repo.activeBooks }.thenReturn(listOf(anotherBook, bookToFind))
    currentBookIdPref = MemoryPref(UUID.randomUUID())

    searchHandler = BookSearchHandler(repo, player, currentBookIdPref)
  }

  @Test
  fun unstructuredSearchByBook() {
    val bookSearch = BookSearch(query = bookToFind.name)
    searchHandler.handle(bookSearch)

    assertThat(currentBookIdPref.value).isEqualTo(bookToFind.id)
    verify(player).play()
    verifyNoMoreInteractions(player)
  }

  @Test
  fun unstructuredSearchByArtist() {
    val bookSearch = BookSearch(query = bookToFind.author)
    searchHandler.handle(bookSearch)

    assertThat(currentBookIdPref.value).isEqualTo(bookToFind.id)
    verify(player).play()
    verifyNoMoreInteractions(player)
  }

  @Test
  fun unstructuredSearchByChapter() {
    val bookSearch = BookSearch(query = bookToFindChapter1.name)
    searchHandler.handle(bookSearch)

    assertThat(currentBookIdPref.value).isEqualTo(bookToFind.id)
    verify(player).play()
    verifyNoMoreInteractions(player)
  }

  @Test
  fun mediaFocusAnyNoneFoundButPlayed() {
    val bookSearch = BookSearch(mediaFocus = "vnd.android.cursor.item/*")
    searchHandler.handle(bookSearch)

    assertThat(currentBookIdPref.value).isEqualTo(anotherBook.id)
    verify(player).play()
  }

  @Test
  fun mediaFocusArtist() {
    val bookSearch = BookSearch(
      mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
      artist = bookToFind.author
    )
    searchHandler.handle(bookSearch)

    assertThat(currentBookIdPref.value).isEqualTo(bookToFind.id)
    verify(player).play()

    verifyNoMoreInteractions(player)
  }

  @Test
  fun mediaFocusArtistInTitleNoArtistInBook() {
    val bookToFind = bookToFind.updateMetaData { copy(author = null, name = "The book of Tim") }
    given { repo.activeBooks }.thenReturn(listOf(bookToFind))

    val bookSearch = BookSearch(
      mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
      query = "Tim",
      artist = "Tim"
    )
    searchHandler.handle(bookSearch)

    assertThat(currentBookIdPref.value).isEqualTo(bookToFind.id)
    verify(player).play()
    verifyNoMoreInteractions(player)
  }

  @Test
  fun mediaFocusAlbum() {
    val bookSearch = BookSearch(
      mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
      artist = bookToFind.author,
      album = bookToFind.name
    )
    searchHandler.handle(bookSearch)

    assertThat(currentBookIdPref.value).isEqualTo(bookToFind.id)
    verify(player).play()
    verifyNoMoreInteractions(player)
  }

  @Test
  fun mediaFocusPlaylist() {
    val bookSearch = BookSearch(
      mediaFocus = MediaStore.Audio.Playlists.ENTRY_CONTENT_TYPE,
      artist = bookToFind.author,
      playList = bookToFind.name,
      album = bookToFind.name
    )
    searchHandler.handle(bookSearch)

    assertThat(currentBookIdPref.value).isEqualTo(bookToFind.id)
    verify(player).play()
    verifyNoMoreInteractions(player)
  }
}
