package voice.core.playback.session

import androidx.media3.session.CommandButton
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.TestScope
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.data.notification.MediaNotificationPreferences
import voice.core.data.notification.NotificationAction
import voice.core.playback.MemoryDataStore
import voice.core.playback.R
import voice.core.sleeptimer.SleepTimer
import voice.core.sleeptimer.SleepTimerMode
import voice.core.sleeptimer.SleepTimerState
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.minutes

@RunWith(AndroidJUnit4::class)
class MediaButtonPreferencesUpdaterTest {

  private val sleepTimer = FakeSleepTimer()
  private val preferencesStore = MemoryDataStore(MediaNotificationPreferences.Default)
  private val updater = MediaButtonPreferencesUpdater(
    context = ApplicationProvider.getApplicationContext(),
    scope = TestScope(),
    sleepTimer = sleepTimer,
    preferencesStore = preferencesStore,
  )

  @Test
  fun `sleep timer button uses the disabled icon when the timer is off`() {
    val button = updater.preferences().sleepTimerButton()
    assertEquals(expected = R.drawable.ic_sleep_timer, actual = button.iconResId)
  }

  @Test
  fun `sleep timer button uses the enabled icon when the timer is running`() {
    sleepTimer.enable(SleepTimerMode.TimedWithDuration(15.minutes))

    val button = updater.preferences().sleepTimerButton()

    assertEquals(expected = R.drawable.ic_sleep_timer_off, actual = button.iconResId)
  }

  @Test
  fun `slot1 and slot2 default to rewind and fast forward in their fixed slots`() {
    val buttons = updater.preferences()

    assertEquals(expected = CommandButton.SLOT_BACK, actual = buttons[0].slots.get(0))
    assertEquals(expected = CommandButton.SLOT_FORWARD, actual = buttons[1].slots.get(0))
  }

  @Test
  fun `a bookmark action in slot3 renders as a bookmark command button`() {
    val buttons = updater.preferences(
      preferences = MediaNotificationPreferences.Default.copy(slot3 = NotificationAction.BOOKMARK),
    )

    val bookmarkButton = buttons[2]
    assertEquals(
      expected = LibrarySessionCallback.ACTION_BOOKMARK,
      actual = bookmarkButton.sessionCommand?.customAction,
    )
  }

  @Test
  fun `next and previous chapter actions dispatch their dedicated session commands`() {
    val buttons = updater.preferences(
      preferences = MediaNotificationPreferences(
        slot1 = NotificationAction.NEXT_CHAPTER,
        slot2 = NotificationAction.PREVIOUS_CHAPTER,
        slot3 = NotificationAction.SLEEP_TIMER,
      ),
    )

    assertEquals(expected = LibrarySessionCallback.ACTION_NEXT_CHAPTER, actual = buttons[0].sessionCommand?.customAction)
    assertEquals(expected = LibrarySessionCallback.ACTION_PREVIOUS_CHAPTER, actual = buttons[1].sessionCommand?.customAction)
  }

  private fun List<CommandButton>.sleepTimerButton() = single {
    it.sessionCommand?.customAction == LibrarySessionCallback.ACTION_SLEEP_TIMER
  }

  private class FakeSleepTimer : SleepTimer {
    override val state: StateFlow<SleepTimerState>
      get() = stateFlow

    private val stateFlow = MutableStateFlow<SleepTimerState>(SleepTimerState.Disabled)

    override fun enable(mode: SleepTimerMode) {
      stateFlow.value = when (mode) {
        is SleepTimerMode.TimedWithDuration -> SleepTimerState.Enabled.WithDuration(mode.duration)
        SleepTimerMode.TimedWithDefault -> error("TimedWithDefault is not used in these tests")
        SleepTimerMode.EndOfChapter -> SleepTimerState.Enabled.WithEndOfChapter
      }
    }

    override fun disable() {
      stateFlow.value = SleepTimerState.Disabled
    }
  }
}
