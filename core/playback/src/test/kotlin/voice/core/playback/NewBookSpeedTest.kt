package voice.core.playback

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.test.runTest
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

class NewBookSpeedTest {

  private val newBook = content(id = "new")

  @Test
  fun `a new book starts at the speed of the book played last`() = runTest {
    val speed = speedAfterApplying(
      content(id = "earlier", playbackSpeed = 1.5F, lastPlayedAt = Instant.ofEpochSecond(1)),
      content(id = "last", playbackSpeed = 1.75F, lastPlayedAt = Instant.ofEpochSecond(2)),
      newBook,
    )

    assertEquals(expected = 1.75F, actual = speed)
  }

  @Test
  fun `a finished book whose files are gone still counts`() = runTest {
    val speed = speedAfterApplying(
      content(id = "active", playbackSpeed = 1.25F, lastPlayedAt = Instant.ofEpochSecond(1)),
      content(id = "deleted", playbackSpeed = 2F, lastPlayedAt = Instant.ofEpochSecond(2), isActive = false),
      newBook,
    )

    assertEquals(expected = 2F, actual = speed)
  }

  @Test
  fun `a book that was played keeps its speed`() = runTest {
    val speed = speedAfterApplying(
      content(id = "last", playbackSpeed = 2F, lastPlayedAt = Instant.ofEpochSecond(2)),
      newBook.copy(lastPlayedAt = Instant.ofEpochSecond(1)),
    )

    assertEquals(expected = 1F, actual = speed)
  }

  @Test
  fun `a speed picked before the first play stays`() = runTest {
    val speed = speedAfterApplying(
      content(id = "last", playbackSpeed = 2F, lastPlayedAt = Instant.ofEpochSecond(2)),
      newBook.copy(playbackSpeed = 1.25F),
    )

    assertEquals(expected = 1.25F, actual = speed)
  }

  @Test
  fun `without a played book a new book starts at normal speed`() = runTest {
    val speed = speedAfterApplying(
      content(id = "other new", playbackSpeed = 1.5F),
      newBook,
    )

    assertEquals(expected = 1F, actual = speed)
  }

  private suspend fun speedAfterApplying(vararg contents: BookContent): Float {
    val contentRepo = MemoryBookContentRepo(*contents)
    val bookRepository = mockk<BookRepository> {
      coEvery { updateBook(any(), any()) } coAnswers {
        val update = secondArg<(BookContent) -> BookContent>()
        contentRepo.put(update(contentRepo.get(firstArg())!!))
      }
    }
    NewBookSpeed(contentRepo, bookRepository).applyTo(newBook.id)
    return contentRepo.get(newBook.id)!!.playbackSpeed
  }
}

private fun content(
  id: String,
  playbackSpeed: Float = 1F,
  lastPlayedAt: Instant = Instant.EPOCH,
  isActive: Boolean = true,
): BookContent {
  val chapter = ChapterId("$id-chapter")
  return BookContent(
    id = BookId(id),
    playbackSpeed = playbackSpeed,
    skipSilence = false,
    isActive = isActive,
    lastPlayedAt = lastPlayedAt,
    author = null,
    name = id,
    addedAt = Instant.EPOCH,
    chapters = listOf(chapter),
    currentChapter = chapter,
    positionInChapter = 0,
    cover = null,
    gain = 0F,
    genre = null,
    narrator = null,
    series = null,
    part = null,
  )
}

private class MemoryBookContentRepo(vararg contents: BookContent) : BookContentRepo {

  private val contents = MutableStateFlow(contents.toList())

  override fun flow(): Flow<List<BookContent>> = contents

  override suspend fun all(): List<BookContent> = contents.value

  override fun flow(id: BookId): Flow<BookContent?> = contents.map { contents -> contents.find { it.id == id } }

  override suspend fun get(id: BookId): BookContent? = contents.value.find { it.id == id }

  override suspend fun setAllInactiveExcept(ids: List<BookId>) {
    contents.update { contents ->
      contents.map { it.copy(isActive = it.id in ids) }
    }
  }

  override suspend fun put(content: BookContent) {
    contents.update { contents ->
      contents.filter { it.id != content.id } + content
    }
  }
}
