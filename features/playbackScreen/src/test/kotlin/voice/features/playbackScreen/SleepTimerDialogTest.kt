package voice.features.playbackScreen

import android.content.Context
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.strings.R
import voice.core.ui.VoiceTheme
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class SleepTimerDialogTest {

  @get:Rule
  val compose = createComposeRule()

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private var increments = 0

  private fun showSheet() {
    compose.setContent {
      VoiceTheme {
        SleepTimerDialog(
          customSleepTime = 15,
          onDismiss = {},
          onIncrementSleepTime = { increments++ },
          onDecrementSleepTime = {},
          onAcceptSleepTime = {},
          onAcceptSleepAtEndOfChapter = {},
        )
      }
    }
    compose.waitForIdle()
  }

  private fun increment() = compose.onNodeWithContentDescription(context.getString(R.string.sleep_timer_dialog_action_increment))

  @Test
  fun `a tap adds one minute`() {
    showSheet()

    increment().performClick()
    compose.mainClock.advanceTimeBy(2_000)

    assertEquals(1, increments)
  }

  @Test
  fun `holding repeats until released`() {
    showSheet()

    increment().performTouchInput { down(center) }
    compose.mainClock.advanceTimeBy(1_000)
    increment().performTouchInput { up() }
    val whileHeld = increments
    compose.mainClock.advanceTimeBy(2_000)

    assertTrue(whileHeld > 1)
    assertEquals(whileHeld, increments)
  }

  @Test
  fun `holding a little longer than a tap still adds a minute`() {
    showSheet()

    increment().performTouchInput { down(center) }
    compose.mainClock.advanceTimeBy(700)
    increment().performTouchInput { up() }
    compose.mainClock.advanceTimeBy(2_000)

    assertTrue(increments >= 1)
  }

  @Test
  fun `accessibility services click once and have no long click that repeats`() {
    showSheet()

    increment()
      .assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnLongClick))
      .performSemanticsAction(SemanticsActions.OnClick)
    compose.mainClock.advanceTimeBy(2_000)

    assertEquals(1, increments)
  }
}
