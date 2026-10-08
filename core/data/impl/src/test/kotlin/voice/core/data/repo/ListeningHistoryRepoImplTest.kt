package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.internals.AppDb
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class ListeningHistoryRepoImplTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val now = Instant.parse("2026-10-07T10:00:00Z")
  private val repo = ListeningHistoryRepoImpl(
    dao = db.listeningEventDao(),
    appDb = db,
  )
  private val bookId = BookId("book")

  @After
  fun tearDown() {
    db.close()
  }

  private fun event(
    type: ListeningEvent.Type,
    at: Instant,
    bookId: BookId = this.bookId,
    chapterId: ChapterId = ChapterId("chapter"),
    toChapterId: ChapterId? = null,
  ) = ListeningEvent(
    bookId = bookId,
    type = type,
    source = ListeningEvent.Source.App,
    atMillis = at.toEpochMilli(),
    chapterId = chapterId,
    time = 0,
    toChapterId = toChapterId,
  )

  private fun ago(duration: Duration): Instant = now.minusMillis(duration.inWholeMilliseconds)

  private suspend fun types(): List<ListeningEvent.Type> = repo.events(bookId).first().map { it.type }

  @Test
  fun `events of the book come newest first`() = runTest {
    repo.add(event(ListeningEvent.Type.Play, ago(10.minutes)))
    repo.add(event(ListeningEvent.Type.Pause, ago(5.minutes)))
    repo.add(event(ListeningEvent.Type.Play, now, bookId = BookId("other")))

    assertEquals(expected = listOf(ListeningEvent.Type.Pause, ListeningEvent.Type.Play), actual = types())
  }

  @Test
  fun `only the newest events of each book are kept`() = runTest {
    repo.add(event(ListeningEvent.Type.Play, ago(1000.minutes)))
    repeat(ListeningHistoryRepoImpl.MAX_EVENTS_PER_BOOK) { index ->
      repo.add(event(ListeningEvent.Type.Pause, ago((999 - index).minutes)))
    }
    repo.add(event(ListeningEvent.Type.Play, ago(2000.minutes), bookId = BookId("other")))

    assertEquals(
      expected = List(ListeningHistoryRepoImpl.MAX_EVENTS_PER_BOOK) { ListeningEvent.Type.Pause },
      actual = types(),
    )
    assertEquals(expected = 1, actual = repo.events(BookId("other")).first().size)
  }

  @Test
  fun `a moved book takes the history of its chapters along`() = runTest {
    val oldChapters = List(3) { ChapterId("old/$it") }
    val newChapters = List(2) { ChapterId("new/$it") }
    val newBook = BookId("new")
    val unrelatedBook = BookId("unrelated")
    repo.add(event(ListeningEvent.Type.Play, ago(3.minutes), chapterId = oldChapters[0]))
    repo.add(
      event(ListeningEvent.Type.ChapterChange, ago(2.minutes), chapterId = oldChapters[0], toChapterId = oldChapters[1]),
    )
    repo.add(event(ListeningEvent.Type.Pause, ago(1.minutes), chapterId = oldChapters[2]))
    repo.add(event(ListeningEvent.Type.Play, now, bookId = unrelatedBook, chapterId = oldChapters[0]))

    repo.moveToBook(
      from = listOf(bookId),
      to = newBook,
      chapters = mapOf(oldChapters[0] to newChapters[0], oldChapters[1] to newChapters[1]),
    )

    assertEquals(
      expected = listOf(
        Triple(ListeningEvent.Type.ChapterChange, newChapters[0], newChapters[1]),
        Triple(ListeningEvent.Type.Play, newChapters[0], null),
      ),
      actual = repo.events(newBook).first().map { Triple(it.type, it.chapterId, it.toChapterId) },
    )
    // the chapter that is not part of the new book stays where it was
    assertEquals(
      expected = listOf(oldChapters[2]),
      actual = repo.events(bookId).first().map { it.chapterId },
    )
    assertEquals(
      expected = listOf(oldChapters[0]),
      actual = repo.events(unrelatedBook).first().map { it.chapterId },
    )
  }

  @Test
  fun `a book that keeps its chapters under a new id takes its history along`() = runTest {
    val chapter = ChapterId("chapter")
    val newBook = BookId("new")
    repo.add(event(ListeningEvent.Type.Play, now, chapterId = chapter))

    repo.moveToBook(from = listOf(bookId), to = newBook, chapters = mapOf(chapter to chapter))

    assertEquals(expected = emptyList(), actual = types())
    assertEquals(
      expected = listOf(ListeningEvent.Type.Play),
      actual = repo.events(newBook).first().map { it.type },
    )
  }
}
