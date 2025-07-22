package voice.playback.session.button

import android.util.Log
import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlin.time.Duration.Companion.milliseconds

abstract class AbstractMediaButtonHandler(
  protected val scope: CoroutineScope,
  protected val playingStatusCallback: (playing:Boolean?) -> Boolean,
  protected var stopCallback: () -> Unit,
  protected var clickActions: MutableList<MediaButtonHandlerClickAction> = mutableListOf(),
  protected var holdActions: MutableList<MediaButtonHandlerClickAction> = mutableListOf(),
  ): MediaButtonHandler {

  override var handlerDelay = 1050.milliseconds
  val handlerDelayWithoutHoldSupport = 650.milliseconds
  var clickCount = 0
  var buttonReleasedJob: Job? = null

  private var lastAction: MediaButtonHandlerClickAction? = null


  fun log(message: String) {
    Log.d("MediaButtonHandler", message)
  }

  protected fun keyEventToString(keyEvent: KeyEvent?): String {
    if(keyEvent == null) {
      return "keyEvent is <null>"
    }
    val action = when (keyEvent.action) {
      KeyEvent.ACTION_UP -> "ACTION_UP"
      KeyEvent.ACTION_DOWN -> "ACTION_DOWN"
      else -> "ACTION_UNKNOWN"
    }
    val keyCode = when (keyEvent.keyCode) {
      KeyEvent.KEYCODE_HEADSETHOOK -> "KEYCODE_HEADSETHOOK"
      KeyEvent.KEYCODE_MEDIA_PLAY -> "KEYCODE_MEDIA_PLAY"
      KeyEvent.KEYCODE_MEDIA_PAUSE -> "KEYCODE_MEDIA_PAUSE"
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> "KEYCODE_MEDIA_PLAY_PAUSE"
      KeyEvent.KEYCODE_MEDIA_NEXT -> "KEYCODE_MEDIA_NEXT"
      KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "KEYCODE_MEDIA_PREVIOUS"
      KeyEvent.KEYCODE_MEDIA_STOP -> "KEYCODE_MEDIA_STOP"
      else -> "KEYCODE_UNKNOWN"
    }
    return "keyCode=$keyCode, action=$action, repeatCount=${keyEvent.repeatCount}, eventTime=${keyEvent.eventTime}, downTime=${keyEvent.downTime}"
  }

  override fun addClickAction(clicks: Int, callback: () -> Unit) {
    clickActions.add(MediaButtonHandlerClickAction(clicks, callback))
  }

  override fun addHoldAction(clicksBeforeHold: Int, callback: () -> Unit) {
    holdActions.add(MediaButtonHandlerClickAction(clicksBeforeHold, callback))
  }
  fun executeHoldAction(clickCount: Int) {
    log("executeHoldAction: clickCount=$clickCount")
    val action = holdActions.find{it -> it.clicks == clickCount}
    if(action == null) {
      log("executeHoldAction: no action found")
    }
    // progressive actions (like fastForward and rewind) only get called once
    if(action?.progressive == false || action != lastAction) {
      action?.callback?.invoke()
    }
  }

  fun executeClickAction(clickCount: Int) {
    log("executeClickAction: clickCount=$clickCount")

    val action = clickActions.find{it -> it.clicks == clickCount}
    if(action == null) {
      log("executeClickAction: no action found")
    }
    action?.callback?.invoke()
  }

  fun updateClickCount(keyEvent: KeyEvent): KeyCodeResult {
    when (keyEvent.keyCode) {
      KeyEvent.KEYCODE_HEADSETHOOK,
      KeyEvent.KEYCODE_MEDIA_PLAY,
      KeyEvent.KEYCODE_MEDIA_PAUSE,
      KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE -> {
        clickCount++
        log("=== handleCallMediaButton: Headset Hook/Play/ Pause, clickCount=$clickCount")
      }

      KeyEvent.KEYCODE_MEDIA_NEXT -> {
        clickCount += 2
        log("=== handleCallMediaButton: Media Next, clickCount=$clickCount")
      }

      KeyEvent.KEYCODE_MEDIA_PREVIOUS -> {
        clickCount += 3
        log("=== handleCallMediaButton: Media Previous, clickCount=$clickCount")
      }
      KeyEvent.KEYCODE_MEDIA_STOP -> {
        log("=== handleCallMediaButton: Media Stop, clickCount=$clickCount")
        stopCallback.invoke()
        buttonReleasedJob?.cancel()
        return KeyCodeResult.StopPlayback
      } else -> {
      log("=== KeyCode:${keyEvent.keyCode}, clickCount=$clickCount")
      return KeyCodeResult.NotHandled
    }
    }
    return KeyCodeResult.Default
  }
}
