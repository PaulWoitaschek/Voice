package voice.playback.session.button

import android.view.KeyEvent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

class KeyDownHandler(
  scope: CoroutineScope,
  playingStatusCallback: (playing:Boolean?) -> Boolean,
  stopCallback: () -> Unit,
  clickActions: MutableList<MediaButtonHandlerClickAction> = mutableListOf(),
  holdActions: MutableList<MediaButtonHandlerClickAction> = mutableListOf()
) : AbstractMediaButtonHandler(scope, playingStatusCallback, stopCallback, clickActions, holdActions) {

  val holdEndedDelay = 150.milliseconds
  val holdActionDelay = 850.milliseconds
  var buttonHoldEndedJob: Job? = null
  var repetitiveHoldJob: Job? = null
  var wasPlaying: Boolean = false
  var firstRepeatCount = 0


  override fun handleKeyEvent(keyEvent: KeyEvent?): Boolean {
    // ignore all events that are not KEY_DOWN
    if(keyEvent?.action != KeyEvent.ACTION_DOWN) {
      log("handleKeyEvent: IGNORE ${keyEventToString(keyEvent)}, clickCount=$clickCount")

      // mark event as handled
      return true
    }

    log("handleKeyEvent: ${keyEventToString(keyEvent)}, clickCount=$clickCount")

    // timerdelay has +100ms to regard the time of a release before a hold
    // sequence KEY_DOWN starts with handlerDelay, releasing takes 250ms and holding down takes 1000ms
    // so DOWN + UP + DOWN_AND_HOLD sequence may need 1250ms or even more
    val timerDelay = if(holdActions.isEmpty()) handlerDelayWithoutHoldSupport else handlerDelay + holdEndedDelay
    val isRepeatedEvent = keyEvent.repeatCount > 0
    val isFirstRepeatedEvent = isRepeatedEvent && firstRepeatCount == 0
    // only increase the clickCount on non-clickPressed events
    if(isFirstRepeatedEvent) {
      wasPlaying = playingStatusCallback(null)
      firstRepeatCount = keyEvent.repeatCount
      log("firstRepeatedEvent, firstRepeatCount=$firstRepeatCount ")
    } else if(!isRepeatedEvent) {
      firstRepeatCount = 0
      updateClickCount(keyEvent)
      log("no repeatedEvent, updated clickCount=$clickCount ")
    }

    if(isRepeatedEvent) {
      // first we need to cancel a possibly pending buttonReleaseJob
      buttonReleasedJob?.cancel()

      // then define what happens after the key is released after holding down
      // this is also a delayed job, because KEY_UP is completely ignored
      buttonHoldEndedJob?.cancel()
      buttonHoldEndedJob = scope.launch {
        log("buttonHoldEndedJob: scheduled")
        delay(holdEndedDelay)
        log("buttonHoldEndedJob: execute")

        // cancel repetiveHoldJob to prevent it from executing after
        repetitiveHoldJob?.cancel()
        buttonReleasedJob?.cancel()
        clickCount = 0
        log("buttonHoldEndedJob: wasPlaying=$wasPlaying")
        playingStatusCallback(wasPlaying)
      }

      // if the repetitive job has already been queued before and is not completed yet, do nothing
      if(repetitiveHoldJob?.isCompleted == false) {
        log("repetitiveHoldJob still running, do nothing")
        return true
      }

      // if job is null or is completed, requeue executeHoldAction with same amount of clicks
      repetitiveHoldJob = scope.launch {
        log("repetitiveHoldJob queued with clickCount=$clickCount")
        // delay only if not first execution
        if(!isFirstRepeatedEvent) {
          delay(holdActionDelay)
        }
        executeHoldAction(clickCount)
        log("repetitiveHoldJob executed with clickCount=$clickCount")
      }
      return true
    }

    // cancel all running jobs on a new click
    repetitiveHoldJob?.cancel()
    buttonReleasedJob?.cancel()
    buttonHoldEndedJob?.cancel()

    wasPlaying = playingStatusCallback(null) // player.isPlaying
    buttonReleasedJob = scope.launch {
      // delay(650);
      log("clickReleasedJob scheduled: delay=${timerDelay.inWholeMilliseconds}ms, clicks=$clickCount, ${
        keyEventToString(keyEvent)
      }"
      )
      delay(timerDelay)
      buttonHoldEndedJob?.cancel()
      repetitiveHoldJob?.cancel()
      log("clickReleasedJob executed: delay=${timerDelay.inWholeMilliseconds}ms, clicks=$clickCount, ${
        keyEventToString(keyEvent)
      }")
      executeClickAction(clickCount)
      clickCount = 0
    }

    return true
  }

}
