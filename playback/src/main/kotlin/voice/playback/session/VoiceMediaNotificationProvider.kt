package voice.playback.session

import android.content.Context
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.DefaultMediaNotificationProvider
import androidx.media3.session.MediaSession
import com.google.common.collect.ImmutableList
import javax.inject.Inject

class VoiceMediaNotificationProvider
@Inject constructor(context: Context) : DefaultMediaNotificationProvider(context) {

  override fun getMediaButtons(
    session: MediaSession,
    playerCommands: Player.Commands,
    customLayout: ImmutableList<CommandButton>,
    showPauseButton: Boolean,
  ): ImmutableList<CommandButton> {
    return super.getMediaButtons(session, playerCommands, customLayout, showPauseButton)
      .apply {
        forEachIndexed { index, commandButton ->
          /**
           * This shows the previous / next icons in compact mode for Android < 13
           * https://github.com/PaulWoitaschek/Voice/issues/1904
           */
          commandButton?.extras?.putInt(COMMAND_KEY_COMPACT_VIEW_INDEX, index)
        }
      }
  }
}
