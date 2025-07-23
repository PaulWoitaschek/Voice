package voice.playback.session.button

import android.view.KeyEvent
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

interface MediaButtonHandler {
  var handlerDelay: Duration

  fun handleKeyEvent(keyEvent: KeyEvent?): Boolean
  fun addClickAction(clicks: Int, callback: () -> Unit)
  fun addHoldAction(clicksBeforeHold: Int, callback: () -> Unit)
}
