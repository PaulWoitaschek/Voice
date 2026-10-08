package voice.core.playback.stats

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningSession
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.repo.ListeningSummary
import voice.core.playback.session.MediaId
import java.time.Clock
import java.time.Instant
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@RunWith(AndroidJUnit4::class)
class ListeningSessionRecorderTest {

  private val scope = TestScope()
  private val start = Instant.parse("2026-10-07T18:00:00Z")
  private val clock = SchedulerClock(scope.testScheduler, start, ZoneId.of("Europe/Berlin"))
  private val repo = MemoryListeningStatsRepo()
  private val recorder = ListeningSessionRecorder(repo, clock, scope.backgroundScope)
  private val dune = BookId("dune")
  private val hyperion = BookId("hyperion")

  private fun session(
    started: Duration,
    ended: Duration,
    listened: Duration,
    audio: Duration = listened,
    reachedEnd: Boolean = false,
    bookId: BookId = dune,
    id: Long = 1,
  ) = ListeningSession(
    bookId = bookId,
    startedAtMillis = start.toEpochMilli() + started.inWholeMilliseconds,
    endedAtMillis = start.toEpochMilli() + ended.inWholeMilliseconds,
    listenedMillis = listened.inWholeMilliseconds,
    audioMillis = audio.inWholeMilliseconds,
    // summer time in Berlin
    utcOffsetSeconds = 2.hours.inWholeSeconds.toInt(),
    reachedEnd = reachedEnd,
    id = id,
  )

  @Test
  fun `listening is saved as one session`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(10.minutes)
    recorder.stopped()
    runCurrent()

    assertEquals(expected = listOf(session(started = 0.minutes, ended = 10.minutes, listened = 10.minutes)), actual = repo.all)
  }

  @Test
  fun `the session is saved while playing`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(31.seconds)

    assertEquals(expected = listOf(session(started = 0.minutes, ended = 30.seconds, listened = 30.seconds)), actual = repo.all)
  }

  @Test
  fun `the audio time follows the playback speed`() = scope.runTest {
    recorder.playing(dune, speed = 1.5F)
    advanceTimeBy(4.minutes)
    recorder.speedChanged(2F)
    advanceTimeBy(6.minutes)
    recorder.stopped()
    runCurrent()

    assertEquals(
      expected = listOf(session(started = 0.minutes, ended = 10.minutes, listened = 10.minutes, audio = 18.minutes)),
      actual = repo.all,
    )
  }

  @Test
  fun `a short break continues the session`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.stopped()
    advanceTimeBy(3.minutes)
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.stopped()
    runCurrent()

    assertEquals(expected = listOf(session(started = 0.minutes, ended = 13.minutes, listened = 10.minutes)), actual = repo.all)
  }

  @Test
  fun `a long break starts a new session`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.stopped()
    advanceTimeBy(6.minutes)
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.stopped()
    runCurrent()

    assertEquals(
      expected = listOf(
        session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes),
        session(started = 11.minutes, ended = 16.minutes, listened = 5.minutes, id = 2),
      ),
      actual = repo.all,
    )
  }

  @Test
  fun `another book starts a new session`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.playing(hyperion, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.stopped()
    runCurrent()

    assertEquals(
      expected = listOf(
        session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes),
        session(started = 5.minutes, ended = 10.minutes, listened = 5.minutes, bookId = hyperion, id = 2),
      ),
      actual = repo.all,
    )
  }

  @Test
  fun `a changed clock only counts up to a short while`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(10.seconds)
    clock.moved = 3.hours
    advanceTimeBy(21.seconds)
    recorder.stopped()
    runCurrent()

    assertEquals(
      expected = listOf(session(started = 0.minutes, ended = 3.hours + 31.seconds, listened = 2.minutes + 1.seconds)),
      actual = repo.all,
    )
  }

  @Test
  fun `the book that ends finishes the session`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.stopped()
    advanceTimeBy(1.minutes)
    recorder.bookEnded(dune)
    runCurrent()

    assertEquals(
      expected = listOf(session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes, reachedEnd = true)),
      actual = repo.all,
    )
  }

  @Test
  fun `another book that ends doesn't finish the session`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.stopped()
    recorder.bookEnded(hyperion)
    runCurrent()

    assertEquals(expected = listOf(session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes)), actual = repo.all)
  }

  @Test
  fun `a book that ends long after it was listened to doesn't finish the session`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(5.minutes)
    recorder.stopped()
    advanceTimeBy(1.hours)
    recorder.bookEnded(dune)
    runCurrent()

    assertEquals(expected = listOf(session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes)), actual = repo.all)
  }

  @Test
  fun `the player's playback is recorded until the book ends`() = scope.runTest {
    val player = FakePlayer()
    recorder.attachTo(player)

    player.update { it.setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
    player.update { it.setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
    advanceTimeBy(1.minutes)
    player.update { it.setPlaybackState(Player.STATE_ENDED) }
    runCurrent()

    assertEquals(
      expected = listOf(session(started = 0.minutes, ended = 1.minutes, listened = 1.minutes, reachedEnd = true)),
      actual = repo.all,
    )
  }

  private class SchedulerClock(
    private val scheduler: TestCoroutineScheduler,
    private val start: Instant,
    private val zone: ZoneId,
  ) : Clock() {

    // how far the wall clock was moved, e.g. by the listener
    var moved: Duration = Duration.ZERO

    override fun instant(): Instant = start.plusMillis(scheduler.currentTime + moved.inWholeMilliseconds)

    override fun getZone(): ZoneId = zone

    override fun withZone(zone: ZoneId): Clock = SchedulerClock(scheduler, start, zone)
  }

  private class MemoryListeningStatsRepo : ListeningStatsRepo {

    val all = mutableListOf<ListeningSession>()

    override suspend fun save(session: ListeningSession): Long {
      val id = if (session.id == 0L) all.size + 1L else session.id
      val index = all.indexOfFirst { it.id == id }
      if (index == -1) all += session.copy(id = id) else all[index] = session.copy(id = id)
      return id
    }

    override suspend fun summary(
      from: Instant,
      to: Instant,
    ): ListeningSummary = error("Not used")

    override suspend fun moveToBook(
      from: List<BookId>,
      to: BookId,
    ) = error("Not used")
  }

  private class FakePlayer : SimpleBasePlayer(Looper.getMainLooper()) {

    private var state = State.Builder()
      .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
      .setPlaylist(
        listOf(
          MediaItemData.Builder("chapter")
            .setMediaItem(
              MediaItem.Builder()
                .setMediaId(Json.encodeToString(MediaId.serializer(), MediaId.Chapter(BookId("dune"), ChapterId("chapter"))))
                .build(),
            )
            .build(),
        ),
      )
      .setPlaybackState(Player.STATE_READY)
      .setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
      .build()

    init {
      // the first read takes the state, so that the updates are seen as changes
      val _ = playbackState
    }

    fun update(change: (State.Builder) -> State.Builder) {
      state = change(state.buildUpon()).build()
      invalidateState()
      shadowOf(Looper.getMainLooper()).idle()
    }

    override fun getState(): State = state
  }
}
