package voice.core.playback.playstate

import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.serialization.json.Json
import voice.core.data.BookId
import voice.core.data.ListeningSession
import voice.core.data.repo.ListeningSessionRepo
import voice.core.playback.session.MediaId
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

class ListeningSessionTrackerTest {

  // A normal playback should record the matching wall-clock duration.
  @Test
  fun `records played wall clock time when playback pauses`() {
    val fixture = fixture(start = "2024-01-01T10:00:00Z")

    fixture.play()
    fixture.clock.advanceBy(10_000)
    fixture.pause()

    assertEquals(expected = listOf(10_000L), actual = fixture.repo.sessions.map { it.durationMs })
    fixture.release()
  }

  // When the tracker takes over an already playing book, it should open the first segment immediately.
  @Test
  fun `creates a session when tracker attaches to an already playing book`() {
    val fixture = fixture(
      start = "2024-01-01T10:00:00Z",
      initiallyPlaying = true,
    )

    fixture.clock.advanceBy(10_000)
    fixture.pause()

    assertEquals(expected = listOf(10_000L), actual = fixture.repo.sessions.map { it.durationMs })
    fixture.release()
  }

  // Accidental sub-threshold playback must not produce visible stats or pollute history.
  @Test
  fun `ignores accidental playback below threshold`() {
    val fixture = fixture(start = "2024-01-01T10:00:00Z")

    fixture.play()
    fixture.clock.advanceBy(2_000)
    fixture.pause()

    assertEquals(expected = emptyList(), actual = fixture.repo.sessions)
    fixture.release()
  }

  // Playback crossing midnight must be split into natural-day segments for accurate day totals.
  @Test
  fun `splits sessions at local day boundaries`() {
    val fixture = fixture(start = "2024-01-01T23:59:50Z")

    fixture.play()
    fixture.clock.advanceBy(20_000)
    fixture.pause()

    assertEquals(expected = listOf(10_000L, 10_000L), actual = fixture.repo.sessions.map { it.durationMs })
    fixture.release()
  }

  // Switching books must close the previous segment before opening a new one.
  @Test
  fun `switching books closes the previous session`() {
    val fixture = fixture(start = "2024-01-01T10:00:00Z")
    val secondBookId = BookId("second")

    fixture.play()
    fixture.clock.advanceBy(10_000)
    fixture.currentBookId = secondBookId
    fixture.tracker.onMediaItemTransition(mediaItem(secondBookId), Player.MEDIA_ITEM_TRANSITION_REASON_AUTO)
    fixture.scope.runCurrent()
    fixture.clock.advanceBy(5_000)
    fixture.pause()

    assertEquals(
      expected = listOf(BookId("book") to 10_000L, secondBookId to 5_000L),
      actual = fixture.repo.sessions.map { it.bookId to it.durationMs },
    )
    fixture.release()
  }

  // The fixture bundles the player, clock, and repo fake together to focus on the collection state machine.
  private fun fixture(
    start: String,
    initiallyPlaying: Boolean = false,
  ): Fixture {
    val scope = TestScope(UnconfinedTestDispatcher())
    val clock = MutableClock(Instant.parse(start))
    val repo = RecordingListeningSessionRepo()
    val playStateManager = PlayStateManager()
    if (initiallyPlaying) {
      playStateManager.playState = PlayStateManager.PlayState.Playing
    }
    val currentBook = CurrentBook(BookId("book"))
    val player = mockk<Player>(relaxed = true) {
      every { currentMediaItem } answers { mediaItem(currentBook.bookId) }
    }
    val tracker = ListeningSessionTracker(
      repo = repo,
      scope = scope,
      playStateManager = playStateManager,
      clock = clock,
    )
    tracker.attachTo(player)
    scope.runCurrent()
    return Fixture(
      scope = scope,
      clock = clock,
      repo = repo,
      playStateManager = playStateManager,
      tracker = tracker,
      currentBook = currentBook,
    )
  }

  private data class Fixture(
    val scope: TestScope,
    val clock: MutableClock,
    val repo: RecordingListeningSessionRepo,
    val playStateManager: PlayStateManager,
    val tracker: ListeningSessionTracker,
    private val currentBook: CurrentBook,
  ) {

    var currentBookId: BookId
      get() = currentBook.bookId
      set(value) {
        currentBook.bookId = value
      }

    // Play transitions go through the real PlayStateManager so private tracker methods are not called directly.
    fun play() {
      playStateManager.playState = PlayStateManager.PlayState.Playing
      scope.runCurrent()
    }

    // Pause triggers the final settlement so repository writes can be asserted.
    fun pause() {
      playStateManager.playState = PlayStateManager.PlayState.Paused
      scope.runCurrent()
    }

    // The tracker keeps a persistent flow listener; release it before the test ends to avoid leaking background work.
    fun release() {
      tracker.release()
      scope.runCurrent()
    }
  }

  private data class CurrentBook(var bookId: BookId)

  // Media ids use the same serialization as production code so the parsing path is exercised.
  private fun mediaItem(bookId: BookId): MediaItem {
    return MediaItem.Builder()
      .setMediaId(Json.encodeToString(MediaId.serializer(), MediaId.Book(bookId)))
      .build()
  }

  private class MutableClock(
    private var instant: Instant,
    private val zoneId: ZoneId = ZoneOffset.UTC,
  ) : Clock() {

    // Tests advance time manually to avoid slow, flaky real delays.
    fun advanceBy(durationMs: Long) {
      instant = instant.plusMillis(durationMs)
    }

    // Clock API requires the current zone; production uses it to compute local day boundaries.
    override fun getZone(): ZoneId = zoneId

    // Tests never switch zones; returning a new instance keeps the Clock contract intact.
    override fun withZone(zone: ZoneId): Clock = MutableClock(instant, zone)

    // The instant is advanced explicitly by the test so assertions stay reproducible.
    override fun instant(): Instant = instant
  }

  private class RecordingListeningSessionRepo : ListeningSessionRepo {
    val sessions = mutableListOf<ListeningSession>()

    // Replace semantics overwrite by id, mimicking Room's checkpoint update behavior.
    override suspend fun put(session: ListeningSession) {
      sessions.removeAll { it.id == session.id }
      sessions += session
    }

    // Tracker tests do not cover cleanup; keep an empty implementation to satisfy the interface.
    override suspend fun deleteForBook(bookId: BookId) = Unit

    // Tracker tests only verify writes and never observe the aggregation flow.
    override fun durationFlow(
      bookId: BookId,
      startedAt: Instant,
      endedAt: Instant,
    ): Flow<Long> = flowOf(0L)
  }
}
