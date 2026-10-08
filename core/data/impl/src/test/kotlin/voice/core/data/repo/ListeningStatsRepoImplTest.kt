package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.repo.internals.AppDb
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class ListeningStatsRepoImplTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val repo = ListeningStatsRepoImpl(db.listeningSessionDao())
  private val dune = BookId("dune")
  private val hyperion = BookId("hyperion")

  @After
  fun tearDown() {
    db.close()
  }

  private suspend fun insert(session: ListeningSession) {
    val _ = repo.save(session)
  }

  private fun session(
    startedAt: String,
    listened: Duration,
    bookId: BookId = dune,
    reachedEnd: Boolean = false,
    // Berlin in summer
    utcOffset: Duration = 2.hours,
  ): ListeningSession {
    val started = Instant.parse(startedAt).toEpochMilli()
    return ListeningSession(
      bookId = bookId,
      startedAtMillis = started,
      endedAtMillis = started + listened.inWholeMilliseconds,
      listenedMillis = listened.inWholeMilliseconds,
      audioMillis = listened.inWholeMilliseconds,
      utcOffsetSeconds = utcOffset.inWholeSeconds.toInt(),
      reachedEnd = reachedEnd,
    )
  }

  @Test
  fun `saving a session again updates it`() = runTest {
    val session = session("2026-10-07T10:00:00Z", listened = 10.minutes)

    val id = repo.save(session)
    val updatedId = repo.save(session.copy(listenedMillis = 20.minutes.inWholeMilliseconds, id = id))

    assertEquals(expected = id, actual = updatedId)
    assertEquals(expected = 20.minutes, actual = repo.summary().listened)
  }

  @Test
  fun `the summary adds up listening, local days and finished books`() = runTest {
    // still the 5th in UTC, but 00:30 on the 6th in Berlin
    insert(session("2026-10-05T22:30:00Z", listened = 1.hours))
    insert(session("2026-10-06T08:00:00Z", listened = 1.hours, reachedEnd = true))
    insert(session("2026-10-06T10:00:00Z", listened = 1.hours, bookId = hyperion, reachedEnd = true))
    insert(session("2026-10-07T10:00:00Z", listened = 30.minutes, reachedEnd = true))

    assertEquals(
      expected = ListeningSummary(
        listened = 3.hours + 30.minutes,
        listeningDays = 2,
        // newest first, and each book once
        finishedBooks = listOf(
          FinishedBook(dune, at = Instant.parse("2026-10-07T10:30:00Z")),
          FinishedBook(hyperion, at = Instant.parse("2026-10-06T11:00:00Z")),
        ),
      ),
      actual = repo.summary(),
    )
  }

  @Test
  fun `a session without listening is no listening day`() = runTest {
    insert(session("2026-10-07T10:00:00Z", listened = Duration.ZERO, reachedEnd = true))

    assertEquals(expected = 0, actual = repo.summary().listeningDays)
    assertEquals(expected = listOf(dune), actual = repo.summary().finishedBooks.map { it.bookId })
  }

  @Test
  fun `the summary only covers sessions started in its range`() = runTest {
    insert(session("2025-12-31T23:00:00Z", listened = 2.hours))
    insert(session("2026-03-01T10:00:00Z", listened = 1.hours))
    insert(session("2027-01-01T10:00:00Z", listened = 3.hours))

    val summary = repo.summary(from = Instant.parse("2026-01-01T00:00:00Z"), to = Instant.parse("2027-01-01T00:00:00Z"))

    assertEquals(expected = 1.hours, actual = summary.listened)
  }

  @Test
  fun `sessions move to another book`() = runTest {
    insert(session("2026-10-07T10:00:00Z", listened = 1.hours, reachedEnd = true))
    insert(session("2026-10-07T12:00:00Z", listened = 1.hours, bookId = hyperion))

    val merged = BookId("merged")
    repo.moveToBook(from = listOf(dune), to = merged)

    assertEquals(expected = listOf(merged), actual = repo.summary().finishedBooks.map { it.bookId })
    assertEquals(expected = 2.hours, actual = repo.summary().listened)
  }
}
