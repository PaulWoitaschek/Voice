package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.ListeningTally
import voice.core.data.repo.internals.AppDb

@RunWith(AndroidJUnit4::class)
class ListeningStatsRepoImplTest {

  private val db =
    Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .allowMainThreadQueries()
      .build()
  private val repo = ListeningStatsRepoImpl(db.listeningSessionDao(), db.listeningTallyDao())
  private val dune = BookId("dune")
  private val hyperion = BookId("hyperion")
  private val day = LocalDate.parse("2026-10-07")

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
    val started = Instant.parse(startedAt).toEpochMilliseconds()
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
    val updatedId =
      repo.save(session.copy(listenedMillis = 20.minutes.inWholeMilliseconds, id = id))

    assertEquals(expected = id, actual = updatedId)
    assertEquals(expected = 20.minutes, actual = repo.summary().listened)
  }

  @Test
  fun `the summary adds up listening, local days and finished books`() = runTest {
    // still the 5th in UTC, but 00:30 on the 6th in Berlin
    insert(session("2026-10-05T22:30:00Z", listened = 1.hours))
    insert(session("2026-10-06T08:00:00Z", listened = 1.hours, reachedEnd = true))
    insert(
      session("2026-10-06T10:00:00Z", listened = 1.hours, bookId = hyperion, reachedEnd = true),
    )
    insert(session("2026-10-07T10:00:00Z", listened = 30.minutes, reachedEnd = true))

    assertEquals(
      expected =
        ListeningSummary(
          listened = 3.hours + 30.minutes,
          listeningDays = 2,
          // newest first, and each book once
          finishedBooks =
            listOf(
              FinishedBook(dune, at = Instant.parse("2026-10-07T10:30:00Z")),
              FinishedBook(hyperion, at = Instant.parse("2026-10-06T11:00:00Z")),
            ),
        ),
      actual = repo.summary(),
    )
  }

  @Test
  fun `the time listened on some days counts each session on its local day`() = runTest {
    assertEquals(expected = null, actual = repo.listened(day, day.plusDays(1)).first())

    // still the 6th in UTC, but 00:30 on the 7th in Berlin
    insert(session("2026-10-06T22:30:00Z", listened = 1.hours))
    insert(session("2026-10-07T10:00:00Z", listened = 30.minutes))
    insert(session("2026-10-08T10:00:00Z", listened = 2.hours))

    assertEquals(
      expected = 1.hours + 30.minutes,
      actual = repo.listened(day, day.plusDays(1)).first(),
    )
    assertEquals(
      expected = Duration.ZERO,
      actual = repo.listened(day.plusDays(5), day.plusDays(6)).first(),
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

    val summary =
      repo.summary(
        from = Instant.parse("2026-01-01T00:00:00Z"),
        to = Instant.parse("2027-01-01T00:00:00Z"),
      )

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

  @Test
  fun `a tally adds up for each day, book and name`() = runTest {
    repo.add(ListeningTally(day, dune, "listened_on_speaker", 10))
    repo.add(ListeningTally(day, dune, "listened_on_speaker", 5))
    repo.add(ListeningTally(day, dune, "sleep_timer_night", 1))
    repo.add(ListeningTally(day.plusDays(1), dune, "listened_on_speaker", 2))
    repo.add(ListeningTally(day, hyperion, "listened_on_speaker", 3))

    assertEquals(
      expected =
        setOf(
          ListeningTally(day, dune, "listened_on_speaker", 15),
          ListeningTally(day, dune, "sleep_timer_night", 1),
          ListeningTally(day.plusDays(1), dune, "listened_on_speaker", 2),
          ListeningTally(day, hyperion, "listened_on_speaker", 3),
        ),
      actual = tallies(dune, hyperion),
    )
  }

  @Test
  fun `the tally moves to another book and adds up there`() = runTest {
    val merged = BookId("merged")
    repo.add(ListeningTally(day, dune, "sleep_timer_night", 1))
    repo.add(ListeningTally(day, hyperion, "sleep_timer_night", 1))
    repo.add(ListeningTally(day, hyperion, "sleep_timer_shake", 2))
    repo.add(ListeningTally(day, merged, "sleep_timer_night", 1))

    repo.moveToBook(from = listOf(dune, hyperion), to = merged)

    assertEquals(
      expected =
        setOf(
          ListeningTally(day, merged, "sleep_timer_night", 3),
          ListeningTally(day, merged, "sleep_timer_shake", 2),
        ),
      actual = tallies(dune, hyperion, merged),
    )
  }

  private suspend fun tallies(vararg bookIds: BookId): Set<ListeningTally> =
    db.listeningTallyDao().ofBooks(bookIds.toList()).toSet()
}
