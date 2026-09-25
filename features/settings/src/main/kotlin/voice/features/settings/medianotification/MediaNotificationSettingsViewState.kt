package voice.features.settings.medianotification

import voice.core.data.notification.NotificationAction

data class MediaNotificationSettingsViewState(
  val slot1: NotificationAction,
  val slot2: NotificationAction,
  val slot3: NotificationAction,
  val editingSlot: Int?,
)
