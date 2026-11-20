package voice.features.bookOverview.stats

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.time.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.BookId
import voice.core.data.supporter.SupporterBadge
import voice.core.ui.VoiceTheme

@RunWith(AndroidJUnit4::class)
class ListeningStatsScreenTest {

  private val animationsOff =
    object : MotionDurationScale {
      override val scaleFactor = 0F
    }

  @get:Rule val compose = createComposeRule(ComposeUiTestConfig(effectContext = animationsOff))

  private val friday = LocalDate.of(2026, 11, 20)
  private val week =
    ListeningStatsViewState(
      period = PeriodKind.Week.period(friday, back = 0, DayOfWeek.MONDAY),
      current = true,
      canGoBack = false,
      canGoForward = false,
      listened = 3.hours,
      days = 2,
      bars = List(7) { if (it == 0) 2.hours else if (it == 4) 1.hours else Duration.ZERO },
      saved = null,
      countsSince = null,
      finished =
        listOf(
          FinishedBookViewState(BookId("dune"), "Dune", "Frank Herbert", null, friday, true),
        ),
      supporterBadge = null,
    )

  @Test
  fun `animations off in Android's settings, open the screen, the time shows at once`() {
    compose.mainClock.autoAdvance = false
    setContent(week)

    compose.onNodeWithText("3h").assertIsDisplayed()
    compose.onNodeWithText("on 2 of 7 days").assertIsDisplayed()
  }

  @Test
  fun `a week, swipe from left to right, the week before`() {
    var earlier = 0
    setContent(week.copy(canGoBack = true), onEarlierClick = { earlier++ })

    compose.onRoot().performTouchInput { swipeRight() }

    assertEquals(expected = 1, actual = earlier)
  }

  @Test
  fun `a week in a right-to-left language, swipe from right to left, the week before`() {
    var earlier = 0
    setContent(
      week.copy(canGoBack = true),
      onEarlierClick = { earlier++ },
      layoutDirection = LayoutDirection.Rtl,
    )

    compose.onRoot().performTouchInput { swipeLeft() }

    assertEquals(expected = 1, actual = earlier)
  }

  @Test
  fun `the week before, swipe from right to left, the week after`() {
    var later = 0
    setContent(week.copy(current = false, canGoForward = true), onLaterClick = { later++ })

    compose.onRoot().performTouchInput { swipeLeft() }

    assertEquals(expected = 1, actual = later)
  }

  @Test
  fun `a week without listening, open it, nothing here yet, and no support line`() {
    setContent(week.copy(listened = Duration.ZERO, days = 0, finished = emptyList()))

    compose.onNodeWithText("Nothing here yet. Press play, and this fills up.").assertIsDisplayed()
    compose.onNodeWithText("Support development").assertDoesNotExist()
  }

  @Test
  fun `a week with listening, not a supporter, scroll to the end, the support line below the finished books`() {
    setContent(week)

    val support = compose.onNodeWithText("Support development").performScrollTo()
    compose.onNodeWithText("Keeps Voice free and ad-free").assertIsDisplayed()
    val book = compose.onNodeWithText("Dune").getBoundsInRoot()
    assertTrue(support.getBoundsInRoot().top > book.bottom)
  }

  @Test
  fun `a week with listening and no finished book, scroll to the end, the support line below the tiles`() {
    setContent(week.copy(finished = emptyList()))

    val support = compose.onNodeWithText("Support development").performScrollTo()
    val tile = compose.onNodeWithText("Books finished").getBoundsInRoot()
    assertTrue(support.getBoundsInRoot().top > tile.bottom)
  }

  @Test
  fun `a supporter, scroll to the end, their badge and Thanks for supporting Voice`() {
    setContent(week.copy(supporterBadge = SupporterBadge.ThreeMonths))

    compose.onNodeWithText("Thanks for supporting Voice").performScrollTo().assertIsDisplayed()
    compose.onNodeWithText("Support development").assertDoesNotExist()
  }

  private fun setContent(
    viewState: ListeningStatsViewState,
    onEarlierClick: () -> Unit = {},
    onLaterClick: () -> Unit = {},
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
  ) {
    compose.setContent {
      CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        VoiceTheme {
          ListeningStatsScreen(
            viewState = viewState,
            onCloseClick = {},
            onKindClick = {},
            onEarlierClick = onEarlierClick,
            onLaterClick = onLaterClick,
            onBookClick = {},
            onSupportClick = {},
          )
        }
      }
    }
  }
}
