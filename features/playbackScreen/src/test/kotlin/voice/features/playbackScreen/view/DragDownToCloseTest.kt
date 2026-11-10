package voice.features.playbackScreen.view

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.TouchInjectionScope
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeDown
import androidx.compose.ui.test.swipeUp
import androidx.navigationevent.NavigationEventDispatcher
import androidx.navigationevent.NavigationEventDispatcherOwner
import androidx.navigationevent.OnBackCompletedFallback
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.mockk
import io.mockk.verify
import kotlin.test.Test
import org.junit.Rule
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class DragDownToCloseTest {

  @get:Rule val compose = createComposeRule()

  private val back = mockk<OnBackCompletedFallback>(relaxUnitFun = true)

  @Test
  fun `a short flick down closes the player`() {
    swipe { swipeDown(startY = top + 100F, endY = top + 200F, durationMillis = 50) }
    verify { back.onBackCompletedFallback() }
  }

  @Test
  fun `a short slow drag keeps the player open`() {
    swipe { swipeDown(startY = top + 100F, endY = top + 150F, durationMillis = 2000) }
    verify(exactly = 0) { back.onBackCompletedFallback() }
  }

  @Test
  fun `a drag up keeps the player open`() {
    swipe { swipeUp(durationMillis = 50) }
    verify(exactly = 0) { back.onBackCompletedFallback() }
  }

  @Test
  fun `a slow drag past the middle closes the player`() {
    swipe { swipeDown(startY = top + 10F, endY = top + height * 0.7F, durationMillis = 5000) }
    verify { back.onBackCompletedFallback() }
  }

  private fun swipe(block: TouchInjectionScope.() -> Unit) {
    val owner =
      object : NavigationEventDispatcherOwner {
        override val navigationEventDispatcher = NavigationEventDispatcher(back)
      }
    compose.setContent {
      CompositionLocalProvider(LocalNavigationEventDispatcherOwner provides owner) {
        Box(Modifier.fillMaxSize().testTag(PLAYER).dragDownToClose(rememberDragDownToCloseState()))
      }
    }
    compose.onNodeWithTag(PLAYER).performTouchInput(block)
    compose.waitForIdle()
  }
}

private const val PLAYER = "player"
