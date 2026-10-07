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
import voice.core.data.repo.internals.MemoryDataStore
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
  private val enabledStore = MemoryDataStore(true)
  private val now = Instant.parse("2026-10-07T10:00:00Z")
  private val repo = ListeningHistoryRepoImpl(
    dao = db.listeningEventDao(),
    enabledStore = enabledStore,
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
  ) = ListeningEvent(
    bookId = bookId,
    type = type,
    source = ListeningEvent.Source.App,
    atMillis = at.toEpochMilli(),
    chapterId = ChapterId("chapter"),
    time = 0,
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
  fun `nothing is recorded while history is off`() = runTest {
    enabledStore.updateData { false }
    repo.add(event(ListeningEvent.Type.Play, now))

    assertEquals(expected = emptyList(), actual = types())
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
  fun `clearing removes everything`() = runTest {
    repo.add(event(ListeningEvent.Type.Play, now))

    repo.clear()

    assertEquals(expected = emptyList(), actual = types())
  }
}
