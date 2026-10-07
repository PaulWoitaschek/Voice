package voice.core.playback.history

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent.Source
import voice.core.data.ListeningEvent.Type
import voice.core.playback.session.MediaId
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class AutomaticPauseRecorderTest {

  private val scope = TestScope()
  private val repo = RecordingRepo()
  private val player = FakePlayer()

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
    return repo.events.map { it.type to it.source }
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

  private class FakePlayer : SimpleBasePlayer(Looper.getMainLooper()) {

    private var state = State.Builder()
      .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
      .setPlaylist(
        listOf(
          MediaItemData.Builder("chapter")
            .setMediaItem(
              MediaItem.Builder()
                .setMediaId(Json.encodeToString(MediaId.serializer(), MediaId.Chapter(BookId("book"), ChapterId("chapter"))))
                .build(),
            )
            .build(),
        ),
      )
      .setContentPositionMs(5_000)
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
