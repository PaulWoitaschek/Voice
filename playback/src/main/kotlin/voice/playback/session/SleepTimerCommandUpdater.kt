package voice.playback.session

import android.content.Context
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaSession
import voice.playback.R
import javax.inject.Inject
import voice.strings.R as StringsR

class SleepTimerCommandUpdater
@Inject constructor(private val context: Context) {

  fun update(
    session: MediaSession,
    controller: MediaSession.ControllerInfo,
    sleepTimerActive: Boolean,
  ) {
    session.setCustomLayout(controller, layout(sleepTimerActive))
  }

  fun update(
    session: MediaSession,
    sleepTimerActive: Boolean,
  ) {
    session.setCustomLayout(layout(sleepTimerActive))
  }

  private fun layout(sleepTimerActive: Boolean): List<CommandButton> {
    return listOf(
      CommandButton.Builder()
        .setSessionCommand(PublishedCustomCommand.Sleep.sessionCommand)
        .setDisplayName(
          context.getString(
            if (sleepTimerActive) {
              StringsR.string.notification_sleep_timer_disable
            } else {
              StringsR.string.notification_sleep_timer_enable
            },
          ),
        )
        .setIconResId(
          if (sleepTimerActive) {
            R.drawable.ic_bedtime_off
          } else {
            R.drawable.ic_bedtime
          },
        )
        .build(),
    )
  }
}
