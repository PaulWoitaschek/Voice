package voice.features.widget

import android.appwidget.AppWidgetManager
import android.content.res.Configuration
import android.os.Bundle
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.runner.RunWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(AndroidJUnit4::class)
class NovaSizeTest {

  @Test
  fun `on nova portrait lays out for the portrait size a bit shorter`() {
    val size = novaSize(options(nova = true), Configuration.ORIENTATION_PORTRAIT)

    assertEquals(DpSize(313.dp, 236.dp * 0.8F), size)
  }

  @Test
  fun `on nova landscape lays out for the landscape size a bit shorter`() {
    val size = novaSize(options(nova = true), Configuration.ORIENTATION_LANDSCAPE)

    assertEquals(DpSize(538.dp, 118.dp * 0.8F), size)
  }

  @Test
  fun `other launchers keep the sizes they report`() {
    assertNull(novaSize(options(nova = false), Configuration.ORIENTATION_PORTRAIT))
  }

  private fun options(nova: Boolean) = Bundle().apply {
    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 313)
    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, 538)
    putInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, 118)
    putInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT, 236)
    if (nova) {
      putInt("com.teslacoilsw.launcher.appWidgetCornerRadius", 16)
    }
  }
}
