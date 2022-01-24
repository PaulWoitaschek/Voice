package de.ph1b.audiobook.playback.session.search

import android.net.Uri
import android.provider.MediaStore
import androidx.datastore.core.DataStore
import androidx.test.ext.junit.runners.AndroidJUnit4
import de.ph1b.audiobook.data.Book2
import de.ph1b.audiobook.data.BookContent2
import de.ph1b.audiobook.data.Chapter2
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.playback.PlayerController
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.updateAndGet
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import java.time.Instant
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BookSearchHandlerTest {

  private val searchHandler: BookSearchHandler

  private val repo = mockk<BookRepo2>()
  private val player = mockk<PlayerController>(relaxUnitFun = true)
  private val currentBookId = MemoryDataStore<Book2.Id?>(null)

  private val anotherBook = book(listOf(chapter(), chapter()))
  private val bookToFind = book(listOf(chapter(), chapter()))

  init {
    coEvery { repo.flow() } returns flowOf(listOf(anotherBook, bookToFind))

    searchHandler = BookSearchHandler(repo, player, currentBookId)
  }

  @Test
  fun unstructuredSearchByBook() = runTest {
    val bookSearch = BookSearch(query = bookToFind.content.name)
    searchHandler.handle(bookSearch)

    currentBookIdShouldBe(bookToFind)
    verify(exactly = 1) { player.play() }
  }

  private suspend fun currentBookIdShouldBe(book2: Book2 = bookToFind) {
    currentBookId.data.first() shouldBe book2.content.uri
  }

  @Test
  fun unstructuredSearchByArtist() = runTest {
    val bookSearch = BookSearch(query = bookToFind.content.author)
    searchHandler.handle(bookSearch)

    currentBookIdShouldBe()
    verify(exactly = 1) { player.play() }
  }

  @Test
  fun unstructuredSearchByChapter() = runTest {
    val bookSearch = BookSearch(query = bookToFind.chapters.first().name)
    searchHandler.handle(bookSearch)

    currentBookIdShouldBe()
    verify(exactly = 1) { player.play() }
  }

  @Test
  fun mediaFocusAnyNoneFoundButPlayed() = runTest {
    val bookSearch = BookSearch(mediaFocus = "vnd.android.cursor.item/*")
    searchHandler.handle(bookSearch)

    currentBookIdShouldBe(anotherBook)
    verify(exactly = 1) { player.play() }
  }

  @Test
  fun mediaFocusArtist() = runTest {
    val bookSearch = BookSearch(
      mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
      artist = bookToFind.content.author
    )
    searchHandler.handle(bookSearch)

    currentBookIdShouldBe()
    verify(exactly = 1) { player.play() }
  }

  @Test
  fun mediaFocusArtistInTitleNoArtistInBook() = runTest {
    val bookToFind = bookToFind.copy(
      content = bookToFind.content.copy(
        author = null,
        name = "The book of Tim")
    )
    coEvery { repo.flow() } returns flowOf(listOf(bookToFind))

    val bookSearch = BookSearch(
      mediaFocus = MediaStore.Audio.Artists.ENTRY_CONTENT_TYPE,
      query = "Tim",
      artist = "Tim"
    )
    searchHandler.handle(bookSearch)

    currentBookIdShouldBe()
    verify(exactly = 1) { player.play() }
  }

  @Test
  fun mediaFocusAlbum() = runTest {
    val bookSearch = BookSearch(
      mediaFocus = MediaStore.Audio.Albums.ENTRY_CONTENT_TYPE,
      artist = bookToFind.content.author,
      album = bookToFind.content.name,
      query = null
    )
    searchHandler.handle(bookSearch)

    currentBookIdShouldBe()
    verify(exactly = 1) { player.play() }
  }
}

fun book(chapters: List<Chapter2>): Book2 {
  return Book2(
    content = BookContent2(
      author = UUID.randomUUID().toString(),
      name = UUID.randomUUID().toString(),
      positionInChapter = 42,
      playbackSpeed = 1F,
      addedAt = Instant.EPOCH,
      chapters = chapters.map { it.uri },
      cover = null,
      currentChapter = chapters.first().uri,
      isActive = true,
      lastPlayedAt = Instant.EPOCH,
      skipSilence = false,
      id = Book2.Id(UUID.randomUUID().toString())
    ),
    chapters = chapters,
  )
}

private fun chapter(): Chapter2 {
  return Chapter2(
    uri = Uri.parse(UUID.randomUUID().toString()),
    name = UUID.randomUUID().toString(),
    duration = 10000,
    fileLastModified = Instant.EPOCH,
    markData = emptyList()
  )
}

private class MemoryDataStore<T>(initial: T) : DataStore<T> {

  private val value = MutableStateFlow(initial)

  override val data: Flow<T> get() = value

  override suspend fun updateData(transform: suspend (t: T) -> T): T {
    return value.updateAndGet { transform(it) }
  }
}
