package voice.core.playback.history

import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.ListeningEvent
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.playback.FakePlayer
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AutomaticPauseRecorderTest {

  private val scope = TestScope()
  private val events = mutableListOf<ListeningEvent>()
  private val repo = mockk<ListeningHistoryRepo> {
    coEvery { add(capture(events)) } just Runs
  }
  private val player = FakePlayer(BookId("book"), contentPositionMs = 5_000)

  init {
    AutomaticPauseRecorder(
      ListeningHistoryRecorder(
        repo = repo,
        clock = Clock.fixed(Instant.parse("2026-10-07T10:00:00Z"), ZoneOffset.UTC),
        scope = scope,
      ),
    ).attachTo(player)
  }

  private suspend fun TestScope.recorded(): List<Pair<Type, Source>> {
    advanceUntilIdle()
    return events.map { it.type to it.source }
  }

  @Test
  fun `a call pauses and playback goes on after it`() = scope.runTest {
    player.update { it.setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) }
    player.update { it.setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_NONE) }

    assertEquals(
      expected = listOf(Type.Pause to Source.AudioFocus, Type.Play to Source.AudioFocus),
      actual = recorded(),
    )
  }

  @Test
  fun `a call that turns into a lasting loss pauses once`() = scope.runTest {
    player.update { it.setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) }
    player.update {
      it
        .setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS)
        .setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_NONE)
    }

    assertEquals(expected = listOf(Type.Pause to Source.AudioFocus), actual = recorded())
  }

  @Test
  fun `pausing during a call does not go on after it`() = scope.runTest {
    player.update { it.setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_TRANSIENT_AUDIO_FOCUS_LOSS) }
    player.update {
      it
        .setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST)
        .setPlaybackSuppressionReason(Player.PLAYBACK_SUPPRESSION_REASON_NONE)
    }

    assertEquals(expected = listOf(Type.Pause to Source.AudioFocus), actual = recorded())
  }

  @Test
  fun `losing the audio focus or unplugging pauses`() = scope.runTest {
    player.update { it.setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS) }
    player.update { it.setPlayWhenReady(true, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
    player.update { it.setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY) }

    assertEquals(
      expected = listOf(Type.Pause to Source.AudioFocus, Type.Pause to Source.Unplugged),
      actual = recorded(),
    )
  }

  @Test
  fun `the end of the book pauses`() = scope.runTest {
    player.update { it.setPlaybackState(Player.STATE_ENDED) }

    assertEquals(expected = listOf(Type.Pause to Source.Unknown), actual = recorded())
  }

  @Test
  fun `the end of the book after a pause pauses once`() = scope.runTest {
    player.update { it.setPlayWhenReady(false, Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) }
    player.update { it.setPlaybackState(Player.STATE_ENDED) }

    assertEquals(expected = emptyList(), actual = recorded())
  }
}
