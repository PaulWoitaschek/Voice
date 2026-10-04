package voice.core.playback.session

import android.content.Context
import android.os.Bundle
import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.MediaLibraryService.MediaLibrarySession
import androidx.media3.session.SessionCommand
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import voice.core.data.notification.MediaNotificationPreferences
import voice.core.data.notification.NotificationAction
import voice.core.data.store.MediaNotificationPreferencesStore
import voice.core.playback.R
import voice.core.playback.di.PlaybackScope
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerState
import voice.core.strings.R as StringsR

@Inject
@SingleIn(PlaybackScope::class)
class MediaButtonPreferencesUpdater(
  private val context: Context,
  private val scope: CoroutineScope,
  private val sleepTimer: SleepTimer,
  @MediaNotificationPreferencesStore
  private val preferencesStore: DataStore<MediaNotificationPreferences>,
) {

  fun attachTo(session: MediaLibrarySession) {
    scope.launch {
      combine(preferencesStore.data, sleepTimer.state, ::preferences).collect { buttons ->
        session.setMediaButtonPreferences(buttons)
      }
    }
  }

  fun preferences(
    preferences: MediaNotificationPreferences = MediaNotificationPreferences.Default,
    sleepTimerState: SleepTimerState = sleepTimer.state.value,
  ): List<CommandButton> = listOf(
    commandButton(preferences.slot1, sleepTimerState, CommandButton.SLOT_BACK),
    commandButton(preferences.slot2, sleepTimerState, CommandButton.SLOT_FORWARD),
    commandButton(preferences.slot3, sleepTimerState, slot = null),
  )

  private fun commandButton(
    action: NotificationAction,
    sleepTimerState: SleepTimerState,
    slot: Int?,
  ): CommandButton {
    val builder = when (action) {
      NotificationAction.REWIND ->
        CommandButton.Builder(CommandButton.ICON_SKIP_BACK)
          .setDisplayName(context.getString(StringsR.string.playback_action_rewind))
          .setPlayerCommand(Player.COMMAND_SEEK_BACK)
      NotificationAction.FAST_FORWARD ->
        CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD)
          .setDisplayName(context.getString(StringsR.string.playback_action_fast_forward))
          .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
      NotificationAction.NEXT_CHAPTER ->
        CommandButton.Builder(CommandButton.ICON_NEXT)
          .setDisplayName(context.getString(StringsR.string.playback_chapter_next))
          .setSessionCommand(SessionCommand(LibrarySessionCallback.ACTION_NEXT_CHAPTER, Bundle.EMPTY))
      NotificationAction.PREVIOUS_CHAPTER ->
        CommandButton.Builder(CommandButton.ICON_PREVIOUS)
          .setDisplayName(context.getString(StringsR.string.playback_chapter_previous))
          .setSessionCommand(SessionCommand(LibrarySessionCallback.ACTION_PREVIOUS_CHAPTER, Bundle.EMPTY))
      NotificationAction.SLEEP_TIMER ->
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
          .setCustomIconResId(if (sleepTimerState.enabled) R.drawable.ic_sleep_timer_off else R.drawable.ic_sleep_timer)
          .setDisplayName(context.getString(StringsR.string.sleep_timer_action_open))
          .setSessionCommand(SessionCommand(LibrarySessionCallback.ACTION_SLEEP_TIMER, Bundle.EMPTY))
      NotificationAction.SKIP_SILENCE ->
        CommandButton.Builder(CommandButton.ICON_UNDEFINED)
          .setCustomIconResId(R.drawable.ic_skip_silence)
          .setDisplayName(context.getString(StringsR.string.playback_option_skip_silence))
          .setSessionCommand(SessionCommand(LibrarySessionCallback.ACTION_SKIP_SILENCE, Bundle.EMPTY))
      NotificationAction.BOOKMARK ->
        CommandButton.Builder(CommandButton.ICON_BOOKMARK_FILLED)
          .setDisplayName(context.getString(StringsR.string.bookmark_title))
          .setSessionCommand(SessionCommand(LibrarySessionCallback.ACTION_BOOKMARK, Bundle.EMPTY))
    }
    return (if (slot != null) builder.setSlots(slot) else builder).build()
  }
}
