package voice.core.data.repo.internals

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.ListeningSession
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class ListeningSessionDaoTest {

  // Aggregation must only count segments for the requested book and period.
  @Test
  fun `durationFlow sums sessions for requested book and range`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDb::class.java,
    ).build()
    val dao = db.listeningSessionDao()
    val bookId = BookId("book")
    val otherBookId = BookId("other")

    dao.put(session(bookId, "2024-01-01T10:00:00Z", 10_000))
    dao.put(session(bookId, "2024-01-01T11:00:00Z", 20_000))
    dao.put(session(bookId, "2024-01-02T10:00:00Z", 30_000))
    dao.put(session(otherBookId, "2024-01-01T10:00:00Z", 40_000))

    val duration = dao.durationFlow(
      bookId = bookId,
      startedAt = Instant.parse("2024-01-01T00:00:00Z"),
      endedAt = Instant.parse("2024-01-02T00:00:00Z"),
    ).first()

    assertEquals(expected = 30_000, actual = duration)
    db.close()
  }

  // The test only cares about aggregation bounds, so minimal sessions are built from a start time and duration.
  private fun session(
    bookId: BookId,
    startedAt: String,
    durationMs: Long,
  ): ListeningSession {
    val start = Instant.parse(startedAt)
    return ListeningSession(
      id = ListeningSession.Id.random(),
      bookId = bookId,
      startedAt = start,
      endedAt = start.plusMillis(durationMs),
      durationMs = durationMs,
    )
  }
}
