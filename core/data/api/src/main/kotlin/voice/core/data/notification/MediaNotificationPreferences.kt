package voice.core.data.notification

import kotlinx.serialization.Serializable

@Serializable
public enum class NotificationAction {
  REWIND,
  FAST_FORWARD,
  NEXT_CHAPTER,
  PREVIOUS_CHAPTER,
  SLEEP_TIMER,
  SKIP_SILENCE,
  BOOKMARK,
}

@Serializable
public data class MediaNotificationPreferences(
  val slot1: NotificationAction,
  val slot2: NotificationAction,
  val slot3: NotificationAction,
) {

  public companion object {
    public val Default: MediaNotificationPreferences = MediaNotificationPreferences(
      slot1 = NotificationAction.REWIND,
      slot2 = NotificationAction.FAST_FORWARD,
      slot3 = NotificationAction.SLEEP_TIMER,
    )
  }
}
