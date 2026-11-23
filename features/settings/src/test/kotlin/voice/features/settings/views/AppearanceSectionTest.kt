package voice.features.settings.views

import android.view.View
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.core.view.ViewCompat
import androidx.core.view.accessibility.AccessibilityNodeInfoCompat
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import org.junit.Rule
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import voice.core.ui.ThemeColorScheme
import voice.core.ui.ThemeMode
import voice.core.ui.VoiceTheme

@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w1400dp-h1400dp")
class AppearanceSectionTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `TalkBack on a palette says its name, radio button and if it is selected`() {
    lateinit var view: View
    compose.setContent {
      view = LocalView.current
      VoiceTheme {
        AppearanceSection(
          themeMode = ThemeMode.FollowSystem,
          themeColorScheme = ThemeColorScheme.VoiceBlue,
          pureBlack = false,
          coverColors = false,
          dynamicColorAvailable = true,
          onThemeModeSelect = {},
          onThemeColorSchemeSelect = {},
          onPureBlackToggle = {},
          onCoverColorsToggle = {},
        )
      }
    }

    fun palette(name: String): AccessibilityNodeInfoCompat {
      val id = compose.onNodeWithContentDescription(name).fetchSemanticsNode().id
      return ViewCompat.getAccessibilityDelegate(view)!!.getAccessibilityNodeProvider(view)!!
        .createAccessibilityNodeInfo(id)!!
    }

    val dynamic = palette("Dynamic color")
    assertEquals("Dynamic color", dynamic.contentDescription)
    assertEquals("android.widget.RadioButton", dynamic.className)
    assertEquals(AccessibilityNodeInfoCompat.CHECKED_STATE_FALSE, dynamic.checked)
    val selected = palette("Voice blue")
    assertEquals("Voice blue", selected.contentDescription)
    assertEquals(AccessibilityNodeInfoCompat.CHECKED_STATE_TRUE, selected.checked)
  }
}
