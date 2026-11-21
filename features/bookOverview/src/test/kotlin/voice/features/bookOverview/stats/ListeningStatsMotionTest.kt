package voice.features.bookOverview.stats

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertTrue
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ListeningStatsMotionTest {

  @get:Rule val compose = createComposeRule()

  @Test
  fun `animations on, open the screen, the shape of the name turns`() {
    compose.mainClock.autoAdvance = false
    var rotation = 0F
    compose.setContent { rotation = nameShapeRotation() }

    compose.mainClock.advanceTimeBy(10_000)

    assertTrue(rotation > 0F)
  }
}
