package voice.playback.session.search

import android.provider.MediaStore
import androidx.datastore.core.DataStore
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.test.runTest
import org.junit.Test
import voice.common.BookId
import voice.data.Book
import voice.data.BookContent
import voice.data.Chapter
import voice.data.ChapterId
import voice.data.repo.BookRepository
import java.time.Instant
import java.util.UUID

class BookSearchHandlerTest {

  private val searchHandler: BookSearchHandler

  private val repo = mockk<BookRepository>()
  private val currentBookId = MemoryDataStore<BookId?>(null)

  private val anotherBook = book(listOf(chapter(), chapter()))
  private val bookToFind = book(listOf(chapter(), chapter()))

  init {
    coEvery { repo.all() } coAnswers { listOf(anotherBook, bookToFind) }

    searchHandler = BookSearchHandler(repo, currentBookId)
  }

  @Test
  fun unstructuredSearchByBook() = runTest {
    val bookSearch = VoiceSearch(query = bookToFind.content.name)
    searchHandler.handle(bookSearch) shouldBe bookToFind
  }

  @Test
  fun unstructuredSearchByArtist() = runTest {
    val bookSearch = VoiceSearch(query = bookToFind.content.author)
    searchHandler.handle(bookSearch) shouldBe bookToFind
  }

  @Test
  fun unstructuredSearchByChapter() = runTest {
    val bookSearch = VoiceSearch(query = bookToFind.chapters.first().name)
    searchHandler.handle(bookSearch) shouldBe bookToFind
  }

  @Test
  fun mediaFocusAnyNoneFoundButPlayed() = runTest {
    val bookSearch = VoiceSearch(mediaFocus = "vnd.android.cursor.item/*")
    searchHandler.handle(bookSearch).shouldBeNull()
  }

  @Test
  fun mediaFocusArtist() = runTest {
    val bookSearch = VoiceSearch(
      mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
      artist = bookToFind.content.author,
    )
    searchHandler.handle(bookSearch) shouldBe bookToFind
  }

  @Test
  fun mediaFocusArtistInTitleNoArtistInBook() = runTest {
    val bookToFind = bookToFind.copy(
      content = bookToFind.content.copy(
        author = null,
        name = "The book of Tim",
      ),
    )
    coEvery { repo.all() } coAnswers { listOf(bookToFind) }

    val bookSearch = VoiceSearch(
      mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
      query = "Tim",
      artist = "Tim",
    )
    searchHandler.handle(bookSearch) shouldBe bookToFind
  }

  @Test
  fun mediaFocusAlbum() = runTest {
    val bookSearch = VoiceSearch(
      mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
      artist = bookToFind.content.author,
      album = bookToFind.content.name,
      query = null,
    )
    searchHandler.handle(bookSearch) shouldBe bookToFind
  }
}

fun book(chapters: List<Chapter>): Book {
  return Book(
    content = BookContent(
      author = UUID.randomUUID().toString(),
      name = UUID.randomUUID().toString(),
      positionInChapter = 42,
      playbackSpeed = 1F,
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.id },
      cover = null,
      currentChapter = chapters.first().id,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      skipSilence = false,
      id = BookId(UUID.randomUUID().toString()),
      gain = 0F,
    ),
    chapters = chapters,
  )
}

private fun chapter(): Chapter {
  return Chapter(
    id = ChapterId(UUID.randomUUID().toString()),
    name = UUID.randomUUID().toString(),
    duration = 10000,
    fileLastModified = Instant.EPOCH,
    markData = emptyList(),
  )
}

private class MemoryDataStore<T>(initial: T) : DataStore<T> {

  private val value = MutableStateFlow(initial)

  override val data: Flow<T> get() = value

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    return value.updateAndGet { transform(it) }
  }
}
