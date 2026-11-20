package voice.core.playback.stats

import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.every
import io.mockk.mockk
import java.time.ZoneId
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.ListeningSession
import voice.core.playback.FakePlayer

@RunWith(AndroidJUnit4::class)
class ListeningSessionRecorderTest {

  private val scope = TestScope()
  private val start = Instant.parse("2026-10-07T18:00:00Z")
  private val clock = SchedulerClock(scope.testScheduler, start)
  private val repo = MemoryListeningStatsRepo()
  private val zone = { ZoneId.of("Europe/Berlin") }
  private var output = "speaker"
  private val recorder =
    ListeningSessionRecorder(
      repo = repo,
      clock = clock,
      zone = zone,
      scope = scope.backgroundScope,
      tally = ListeningTallyRecorder(repo, clock, zone, scope.backgroundScope),
      outputDetector = mockk { every { current() } answers { output } },
    )
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
  ) =
    ListeningSession(
      bookId = bookId,
      startedAtMillis = start.toEpochMilliseconds() + started.inWholeMilliseconds,
      endedAtMillis = start.toEpochMilliseconds() + ended.inWholeMilliseconds,
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

    assertEquals(
      expected = listOf(session(started = 0.minutes, ended = 10.minutes, listened = 10.minutes)),
      actual = repo.all,
    )
  }

  @Test
  fun `the session is saved while playing`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(31.seconds)

    assertEquals(
      expected = listOf(session(started = 0.minutes, ended = 30.seconds, listened = 30.seconds)),
      actual = repo.all,
    )
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
      expected =
        listOf(
          session(
            started = 0.minutes,
            ended = 10.minutes,
            listened = 10.minutes,
            audio = 18.minutes,
          ),
        ),
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

    assertEquals(
      expected = listOf(session(started = 0.minutes, ended = 13.minutes, listened = 10.minutes)),
      actual = repo.all,
    )
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
      expected =
        listOf(
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
      expected =
        listOf(
          session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes),
          session(
            started = 5.minutes,
            ended = 10.minutes,
            listened = 5.minutes,
            bookId = hyperion,
            id = 2,
          ),
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
      expected =
        listOf(
          session(
            started = 0.minutes,
            ended = 3.hours + 31.seconds,
            listened = 2.minutes + 1.seconds,
          ),
        ),
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
      expected =
        listOf(
          session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes, reachedEnd = true),
        ),
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

    assertEquals(
      expected = listOf(session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes)),
      actual = repo.all,
    )
  }

  @Test
  fun `a book that ends long after it was listened to doesn't finish the session`() =
    scope.runTest {
      recorder.playing(dune, speed = 1F)
      advanceTimeBy(5.minutes)
      recorder.stopped()
      advanceTimeBy(1.hours)
      recorder.bookEnded(dune)
      runCurrent()

      assertEquals(
        expected = listOf(session(started = 0.minutes, ended = 5.minutes, listened = 5.minutes)),
        actual = repo.all,
      )
    }

  @Test
  fun `the player's playback is recorded until the book ends`() = scope.runTest {
    val player = FakePlayer(dune)
    recorder.attachTo(player)

    player.update { it.setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
    player.update { it.setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
    advanceTimeBy(1.minutes)
    player.update { it.setPlaybackState(Player.STATE_ENDED) }
    runCurrent()

    assertEquals(
      expected =
        listOf(
          session(started = 0.minutes, ended = 1.minutes, listened = 1.minutes, reachedEnd = true),
        ),
      actual = repo.all,
    )
  }

  @Test
  fun `paused, press play on a headset, listen 20 minutes, 20 minutes from a headset`() =
    scope.runTest {
      output = "wired"

      recorder.playPressed(ListeningEvent.Source.Headset)
      listen(20.minutes)

      assertEquals(
        expected = mapOf("listened_from_headset" to 20.minutes, "listened_on_wired" to 20.minutes),
        actual = counted(),
      )
    }

  @Test
  fun `10 minutes after play in the app, pause, press play in the car, listen 20 minutes, 10 minutes from the app, 20 minutes from the car`() =
    scope.runTest {
      recorder.playPressed(ListeningEvent.Source.App)
      listen(10.minutes)

      recorder.playPressed(ListeningEvent.Source.Car)
      listen(20.minutes)

      assertEquals(
        expected =
          mapOf(
            "listened_from_app" to 10.minutes,
            "listened_on_speaker" to 10.minutes,
            "listened_from_car" to 20.minutes,
            "listened_on_car" to 20.minutes,
          ),
        actual = counted(),
      )
    }

  @Test
  fun `the place of the last press stays when playback stops and carries on by itself`() =
    scope.runTest {
      recorder.playPressed(ListeningEvent.Source.Notification)
      listen(10.minutes)

      // a call ended
      listen(5.minutes)

      assertEquals(
        expected =
          mapOf("listened_from_notification" to 15.minutes, "listened_on_speaker" to 15.minutes),
        actual = counted(),
      )
    }

  @Test
  fun `the sleep timer stopped, shake, and listen 15 minutes, 15 minutes from the sleep timer`() =
    scope.runTest {
      recorder.playPressed(ListeningEvent.Source.SleepTimer)
      listen(15.minutes)

      assertEquals(
        expected =
          mapOf("listened_from_sleep_timer" to 15.minutes, "listened_on_speaker" to 15.minutes),
        actual = counted(),
      )
    }

  @Test
  fun `paused, press play on a bluetooth device, listen 20 minutes, 20 minutes from bluetooth`() =
    scope.runTest {
      output = "bluetooth"

      recorder.playPressed(ListeningEvent.Source.Bluetooth)
      listen(20.minutes)

      assertEquals(
        expected =
          mapOf("listened_from_bluetooth" to 20.minutes, "listened_on_bluetooth" to 20.minutes),
        actual = counted(),
      )
    }

  @Test
  fun `bluetooth headphones for 30 minutes, turn them off and play on for 10 minutes on the speaker, 30 minutes on bluetooth, 10 minutes on the phone speaker`() =
    scope.runTest {
      output = "bluetooth"
      listen(30.minutes)

      // Voice pauses when the headphones turn off
      output = "speaker"
      listen(10.minutes)

      assertEquals(
        expected =
          mapOf(
            "listened_from_app" to 40.minutes,
            "listened_on_bluetooth" to 30.minutes,
            "listened_on_speaker" to 10.minutes,
          ),
        actual = counted(),
      )
    }

  @Test
  fun `an output that changes while playing counts from the next save on`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(10.minutes + 10.seconds)

    output = "bluetooth"
    advanceTimeBy(10.minutes)
    recorder.stopped()
    runCurrent()

    assertEquals(
      expected =
        mapOf(
          "listened_from_app" to 20.minutes + 10.seconds,
          "listened_on_speaker" to 10.minutes + 30.seconds,
          "listened_on_bluetooth" to 9.minutes + 40.seconds,
        ),
      actual = counted(),
    )
  }

  @Test
  fun `android auto, listen 40 minutes, 40 minutes in the car`() = scope.runTest {
    // the sound of Android Auto looks like any Bluetooth device
    output = "bluetooth"

    recorder.playPressed(ListeningEvent.Source.Car)
    listen(40.minutes)

    assertEquals(
      expected = mapOf("listened_from_car" to 40.minutes, "listened_on_car" to 40.minutes),
      actual = counted(),
    )
  }

  @Test
  fun `an output that voice can't tell, listen 5 minutes, 5 minutes on other`() = scope.runTest {
    output = "other"

    listen(5.minutes)

    assertEquals(
      expected = mapOf("listened_from_app" to 5.minutes, "listened_on_other" to 5.minutes),
      actual = counted(),
    )
  }

  @Test
  fun `the time before another book starts counts for the book that played`() = scope.runTest {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(10.seconds)

    recorder.playing(hyperion, speed = 1F)
    advanceTimeBy(20.seconds)
    recorder.stopped()
    runCurrent()

    assertEquals(
      expected = mapOf(dune to 10.seconds, hyperion to 20.seconds),
      actual =
        repo.tallies
          .filter { it.name == "listened_on_speaker" }
          .groupBy { it.bookId }
          .mapValues { (_, tallies) -> tallies.sumOf { it.number }.milliseconds },
    )
  }

  @Test
  fun `the time is stored while playing, so a stopped app loses little`() = scope.runTest {
    recorder.playing(dune, speed = 1F)

    advanceTimeBy(65.seconds)
    runCurrent()

    assertEquals(
      expected = mapOf("listened_from_app" to 1.minutes, "listened_on_speaker" to 1.minutes),
      actual = counted(),
    )
  }

  private fun TestScope.listen(duration: Duration) {
    recorder.playing(dune, speed = 1F)
    advanceTimeBy(duration)
    recorder.stopped()
    runCurrent()
  }

  private fun counted(): Map<String, Duration> =
    repo.tallies
      .groupBy { it.name }
      .mapValues { (_, tallies) ->
        tallies.sumOf { it.number }.milliseconds
      }

  private class SchedulerClock(
    private val scheduler: TestCoroutineScheduler,
    private val start: Instant,
  ) : Clock {

    // how far the wall clock was moved, e.g. by the listener
    var moved: Duration = Duration.ZERO

    override fun now(): Instant = start + scheduler.currentTime.milliseconds + moved
  }
}
