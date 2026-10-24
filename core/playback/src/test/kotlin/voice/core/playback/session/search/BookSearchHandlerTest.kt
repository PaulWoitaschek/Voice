package voice.core.playback.session.search

import android.provider.MediaStore
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.BookMetadata
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.MemoryDataStore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.uuid.Uuid

class BookSearchHandlerTest {

  private val searchHandler: BookSearchHandler

  private val repo = mockk<BookRepository>()
  private val currentBookId = MemoryDataStore<BookId?>(null)

  private val anotherBook = book(listOf(chapter(), chapter()))
  private val bookToFind = book(listOf(chapter(), chapter()))

  init {
    coEvery { repo.all() } coAnswers { listOf(anotherBook, bookToFind) }

    searchHandler = BookSearchHandler(repo, CurrentBookStore(currentBookId))
  }

  @Test
  fun unstructuredSearchByBook() = runTest {
    val bookSearch = VoiceSearch(query = bookToFind.content.name)
    assertEquals(expected = bookToFind, actual = searchHandler.handle(bookSearch))
  }

  @Test
  fun unstructuredSearchByArtist() = runTest {
    val bookSearch = VoiceSearch(query = bookToFind.content.metadata.author)
    assertEquals(expected = bookToFind, actual = searchHandler.handle(bookSearch))
  }

  @Test
  fun unstructuredSearchByChapter() = runTest {
    val bookSearch = VoiceSearch(query = bookToFind.chapters.first().name)
    assertEquals(expected = bookToFind, actual = searchHandler.handle(bookSearch))
  }

  @Test
  fun mediaFocusAnyNoneFoundButPlayed() = runTest {
    val bookSearch = VoiceSearch(mediaFocus = "vnd.android.cursor.item/*")
    assertNull(searchHandler.handle(bookSearch))
  }

  @Test
  fun mediaFocusArtist() = runTest {
    val bookSearch = VoiceSearch(
      mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
      artist = bookToFind.content.metadata.author,
    )
    assertEquals(expected = bookToFind, actual = searchHandler.handle(bookSearch))
  }

  @Test
  fun mediaFocusArtistInTitleNoArtistInBook() = runTest {
    val bookToFind = bookToFind.copy(
      content = bookToFind.content.copy(
        metadata = bookToFind.content.metadata.copy(
          author = null,
          name = "The book of Tim",
        ),
      ),
    )
    coEvery { repo.all() } coAnswers { listOf(bookToFind) }

    val bookSearch = VoiceSearch(
      mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
      query = "Tim",
      artist = "Tim",
    )
    assertEquals(expected = bookToFind, actual = searchHandler.handle(bookSearch))
  }

  @Test
  fun mediaFocusAlbum() = runTest {
    val bookSearch = VoiceSearch(
      mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
      artist = bookToFind.content.metadata.author,
      album = bookToFind.content.name,
      query = null,
    )
    assertEquals(expected = bookToFind, actual = searchHandler.handle(bookSearch))
  }
}

fun book(
  chapters: List<Chapter>,
  id: BookId = BookId(Uuid.random().toString()),
  positionInChapter: Long = 0,
): Book {
  return Book(
    content = BookContent(
      positionInChapter = positionInChapter,
      playbackSpeed = 1F,
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.id },
      currentChapter = chapters.first().id,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      skipSilence = false,
      id = id,
      gain = 0F,
      metadata = BookMetadata(
        name = Uuid.random().toString(),
        author = Uuid.random().toString(),
        album = null,
        title = null,
        genre = null,
        narrator = null,
        series = null,
        part = null,
        cover = null,
        fileLastModified = null,
      ),
      customName = null,
      customCover = null,
    ),
    chapters = chapters,
  )
}

private fun chapter(): Chapter {
  return Chapter(
    id = ChapterId(Uuid.random().toString()),
    name = Uuid.random().toString(),
    duration = 10000,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
    fileSize = 0,
  )
}
