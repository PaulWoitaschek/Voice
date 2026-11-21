package voice.features.bookOverview.stats

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.MotionDurationScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.test.ComposeUiTestConfig
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getBoundsInRoot
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeLeft
import androidx.compose.ui.test.swipeRight
import androidx.compose.ui.unit.LayoutDirection
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
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
  private val dune = StatsBookViewState(BookId("dune"), "Dune", cover = null, inLibrary = true)
  private val week =
    ListeningStatsViewState(
      period = PeriodKind.Week.period(friday, back = 0, DayOfWeek.MONDAY),
      current = true,
      canGoBack = false,
      canGoForward = false,
      listened = 3.hours,
      days = 2,
      bars = List(7) { if (it == 0) 2.hours else if (it == 4) 1.hours else Duration.ZERO },
      countsSince = null,
      name = null,
      wideFact = null,
      smallFacts = emptyList(),
      finished = listOf(FinishedBookViewState(dune, friday)),
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
  fun `a week without listening, open it, nothing here yet, with no name, no facts and no support line`() {
    setContent(week.copy(listened = Duration.ZERO, days = 0, finished = emptyList()))

    compose.onNodeWithText("Nothing here yet. Press play, and this fills up.").assertIsDisplayed()
    compose.onNodeWithText("Books finished").assertDoesNotExist()
    compose.onNodeWithText("Support development").assertDoesNotExist()
  }

  @Test
  fun `animations off in Android's settings, open the screen, the shape of the name stands still`() {
    compose.mainClock.autoAdvance = false
    var rotation = -1F
    compose.setContent { rotation = nameShapeRotation() }

    compose.mainClock.advanceTimeBy(10_000)

    assertEquals(expected = 0F, actual = rotation)
  }

  @Test
  fun `a week with 5 h, 70 percent of it between 22_00 and 5_00, open the week, You are a night owl, 70 percent of your listening is at night`() {
    setContent(week.copy(name = ListenerName.NightOwl(percent = 70)))

    compose.onNodeWithText("You are a night owl").assertIsDisplayed()
    compose.onNodeWithText("70% of your listening is at night").assertIsDisplayed()
  }

  @Test
  fun `each name, open the screen, its sentence and the line that says why`() {
    var state by mutableStateOf(week)
    setContent { state }

    mapOf(
        ListenerName.Audiomaniac(2.hours) to ("You are an audiomaniac" to "2h on an average day"),
        ListenerName.MarathonListener(190.minutes) to
          ("You are a marathon listener" to "3h 10m in one go"),
        ListenerName.EarlyBird(45) to
          ("You are an early bird" to "45% of your listening is before 9 in the morning"),
        ListenerName.Commuter to
          ("You are a commuter" to "Mornings and late afternoons on weekdays"),
        ListenerName.Juggler(3) to ("You are a juggler" to "3 books at the same time"),
        ListenerName.Speedster(1.5F) to ("You are a speedster" to "You listen at 1.5×"),
        ListenerName.WeekendListener(70) to
          ("You are a weekend listener" to "70% of your listening is on Saturday and Sunday"),
        ListenerName.CreatureOfHabit to
          ("You are a creature of habit" to "You listened on each day"),
        ListenerName.PartOfDay(DayPart.Morning, 52) to
          ("You are a morning listener" to "52% of your listening is in the morning"),
        ListenerName.PartOfDay(DayPart.Afternoon, 75) to
          ("You are an afternoon listener" to "75% of your listening is in the afternoon"),
        ListenerName.PartOfDay(DayPart.Evening, 52) to
          ("You are an evening listener" to "52% of your listening is in the evening"),
      )
      .forEach { (name, texts) ->
        state = week.copy(name = name)

        compose.onNodeWithText(texts.first).assertIsDisplayed()
        compose.onNodeWithText(texts.second).assertIsDisplayed()
      }
  }

  @Test
  fun `a week with 1 h 30 min, open the week, no name tile`() {
    setContent(week)

    compose.onNodeWithText("You are", substring = true).assertDoesNotExist()
  }

  @Test
  fun `each small fact, open the screen, its number and its line`() {
    var state by mutableStateOf(week)
    setContent { state }
    val november = state.copy(period = PeriodKind.Month.period(friday, back = 0, DayOfWeek.MONDAY))
    val year = state.copy(period = PeriodKind.Year.period(friday, back = 0, DayOfWeek.MONDAY))

    listOf(
        Triple(week, SmallFact.Change(80.minutes), "+1h 20m" to "more than last week"),
        Triple(week, SmallFact.Change(-1.hours), "−1h" to "less than last week"),
        Triple(
          week.copy(current = false),
          SmallFact.Change(-1.hours),
          "−1h" to "less than the week before",
        ),
        Triple(november, SmallFact.Change(80.minutes), "+1h 20m" to "more than last month"),
        Triple(year, SmallFact.Change(80.minutes), "+1h 20m" to "more than last year"),
        Triple(week, SmallFact.LongestDay(2.hours, friday.minusDays(1)), "2h" to "on Thursday"),
        Triple(
          november,
          SmallFact.LongestDay(2.hours, friday.minusDays(1)),
          "2h" to "on Thu, Nov 19",
        ),
        Triple(year, SmallFact.LongestMonth(21.hours, Month.NOVEMBER), "21h" to "in November"),
        Triple(week, SmallFact.Saved(1.hours, speed = 1.5F), "1h" to "saved at 1.5×"),
        Triple(week, SmallFact.LongestStretch(135.minutes), "2h 15m" to "in one go"),
        Triple(november, SmallFact.YourDay(DayOfWeek.SUNDAY), "Sunday" to "is your day"),
        Triple(
          week,
          SmallFact.Streak(current = 4, longest = 4),
          "4 days" to "in a row, your longest streak",
        ),
        Triple(
          week,
          SmallFact.Streak(current = 4, longest = 9),
          "4 days" to "in a row. The longest: 9 days",
        ),
        Triple(
          week,
          SmallFact.Streak(current = 0, longest = 9),
          "9 days" to "is your longest streak",
        ),
      )
      .forEach { (period, fact, texts) ->
        state = period.copy(smallFacts = listOf(fact))

        compose.onNodeWithText(texts.first).performScrollTo().assertIsDisplayed()
        compose.onNodeWithText(texts.second).assertIsDisplayed()
      }
  }

  @Test
  fun `a week with 3 h in the evening and 1 h in the morning, When you listen shows, evening 75 percent, morning 25 percent`() {
    val hours = List(24) { if (it in 18..20) 1.hours else if (it == 8) 1.hours else Duration.ZERO }
    setContent(week.copy(wideFact = WideFact.WhenYouListen(hours)))

    compose.onNodeWithText("When you listen").performScrollTo().assertIsDisplayed()
    compose.onNode(hasText("75%") and hasText("Evening")).performScrollTo().assertIsDisplayed()
    compose.onNode(hasText("25%") and hasText("Morning")).assertIsDisplayed()
    compose.onNode(hasText("0%") and hasText("Night")).assertIsDisplayed()
  }

  @Test
  fun `a week with 3 h of book A and 1 h of book B, Most listened shows, book A with 3h`() {
    setContent(week.copy(listened = 4.hours, wideFact = WideFact.MostListened(dune, 3.hours)))

    compose
      .onNode(hasText("Most listened") and hasText("Dune") and hasText("3h"))
      .performScrollTo()
      .assertIsDisplayed()
  }

  @Test
  fun `Most listened, tap it, the book opens`() {
    var opened: BookId? = null
    setContent(
      week.copy(wideFact = WideFact.MostListened(dune, 2.hours), finished = emptyList()),
      onBookClick = { opened = it },
    )

    compose.onNodeWithText("Most listened").performScrollTo().performClick()

    assertEquals(expected = dune.id, actual = opened)
  }

  @Test
  fun `the most listened book, later removed with its files, Most listened shows, its name without a cover, and no tap`() {
    setContent(
      week.copy(
        wideFact = WideFact.MostListened(dune.copy(inLibrary = false), 2.hours),
        finished = emptyList(),
      ),
    )

    compose.onNodeWithText("Dune").performScrollTo().assertIsDisplayed().assertHasNoClickAction()
    compose.onNodeWithText("Most listened").assertHasNoClickAction()
  }

  @Test
  fun `the forecast, open the screen, the time left and the day`() {
    var state by mutableStateOf(week.copy(finished = emptyList()))
    setContent { state }

    mapOf(
        0L to "At your pace, you finish it today",
        1L to "At your pace, you finish it tomorrow",
        4L to "At your pace, you finish it on Tuesday",
        13L to "At your pace, you finish it on Dec 3",
      )
      .forEach { (days, text) ->
        state =
          state.copy(wideFact = WideFact.Forecast(dune, 9.hours, friday.plusDays(days), friday))

        compose
          .onNode(hasText("9h left") and hasText("Dune") and hasText(text))
          .performScrollTo()
          .assertIsDisplayed()
      }
  }

  @Test
  fun `a week with 3 finished books, open the week, a shelf with 3 covers and the number 3`() {
    setContent(
      week.copy(
        finished =
          listOf("Dune", "Emma", "Ulysses").map {
            FinishedBookViewState(dune.copy(id = BookId(it), name = it), friday)
          },
      ),
    )

    compose.onNodeWithText("Support development").performScrollTo()
    compose.onNodeWithText("Books finished").assertIsDisplayed()
    compose.onNodeWithText("3").assertIsDisplayed()
    listOf("Dune", "Emma", "Ulysses").forEach {
      compose.onNodeWithText(it).performScrollTo().assertIsDisplayed()
    }
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
    onBookClick: (BookId) -> Unit = {},
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
  ) {
    setContent(onEarlierClick, onLaterClick, onBookClick, layoutDirection) { viewState }
  }

  private fun setContent(
    onEarlierClick: () -> Unit = {},
    onLaterClick: () -> Unit = {},
    onBookClick: (BookId) -> Unit = {},
    layoutDirection: LayoutDirection = LayoutDirection.Ltr,
    viewState: () -> ListeningStatsViewState,
  ) {
    compose.setContent {
      CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        VoiceTheme {
          ListeningStatsScreen(
            viewState = viewState(),
            onCloseClick = {},
            onKindClick = {},
            onEarlierClick = onEarlierClick,
            onLaterClick = onLaterClick,
            onBookClick = onBookClick,
            onSupportClick = {},
          )
        }
      }
    }
  }
}
