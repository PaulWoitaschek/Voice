package voice.core.playback

import android.os.Looper
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.SimpleBasePlayer
import kotlinx.serialization.json.Json
import org.robolectric.Shadows.shadowOf
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.playback.session.MediaId

class FakePlayer(
  bookId: BookId,
  contentPositionMs: Long = 0,
) : SimpleBasePlayer(Looper.getMainLooper()) {

  private var state = State.Builder()
    .setAvailableCommands(Player.Commands.Builder().addAllCommands().build())
    .setPlaylist(
      listOf(
        MediaItemData.Builder("chapter")
          .setMediaItem(
            MediaItem.Builder()
              .setMediaId(Json.encodeToString(MediaId.serializer(), MediaId.Chapter(bookId, ChapterId("chapter"))))
              .build(),
          )
          .build(),
      ),
    )
    .setContentPositionMs(contentPositionMs)
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
