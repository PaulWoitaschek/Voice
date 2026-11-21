@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.stats

import android.content.res.Configuration
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavEntry
import coil3.compose.AsyncImage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.Month
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.delay
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.data.supporter.SupporterBadge
import voice.core.strings.R as StringsR
import voice.core.ui.CenteredContent
import voice.core.ui.R as UiR
import voice.core.ui.SupporterBadgeIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.animationsEnabled
import voice.core.ui.bookText
import voice.core.ui.label
import voice.core.ui.rememberCoverThumbnailRequest
import voice.features.bookOverview.views.CoverShape
import voice.navigation.Destination
import voice.navigation.NavEntryProvider

@BindingContainer
@ContributesTo(AppScope::class)
object ListeningStatsProvider {

  @Provides
  @IntoSet
  fun listeningStatsNavEntryProvider(): NavEntryProvider<*> =
    NavEntryProvider<Destination.ListeningStats> { key ->
      NavEntry(key) {
        ListeningStatsScreen()
      }
    }
}

@Composable
fun ListeningStatsScreen() {
  val viewModel = retain {
    rootGraphAs<ListeningStatsViewModel.Provider>().listeningStatsViewModel
  }
  val viewState = viewModel.viewState() ?: return
  ListeningStatsScreen(
    viewState = viewState,
    onCloseClick = viewModel::onCloseClick,
    onKindClick = viewModel::onKindClick,
    onEarlierClick = viewModel::onEarlierClick,
    onLaterClick = viewModel::onLaterClick,
    onBookClick = viewModel::onBookClick,
    onSupportClick = viewModel::onSupportClick,
  )
}

@Composable
internal fun ListeningStatsScreen(
  viewState: ListeningStatsViewState,
  onCloseClick: () -> Unit,
  onKindClick: (PeriodKind) -> Unit,
  onEarlierClick: () -> Unit,
  onLaterClick: () -> Unit,
  onBookClick: (BookId) -> Unit,
  onSupportClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = { Text(stringResource(StringsR.string.listening_stats_title)) },
        navigationIcon = {
          IconButton(onClick = onCloseClick) {
            Icon(
              painterResource(UiR.drawable.ic_arrow_back),
              contentDescription = stringResource(StringsR.string.common_action_back),
            )
          }
        },
      )
    },
  ) { padding ->
    CenteredContent(Modifier.padding(padding)) { horizontalPadding ->
      Column(
        Modifier.fillMaxSize()
          .periodSwipe(viewState, onEarlierClick, onLaterClick)
          .verticalScroll(rememberScrollState())
          .padding(horizontal = horizontalPadding)
          .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        KindChips(viewState.period.kind, onKindClick)
        Hero(viewState, onEarlierClick, onLaterClick)
        if (!viewState.empty) {
          viewState.name?.let { NameTile(it) }
          viewState.wideFact?.let { WideFactTile(it, onBookClick) }
          SmallFactTiles(viewState.smallFacts, viewState.period.kind, viewState.current)
          Shelf(viewState.finished, viewState.period.kind, onBookClick)
          SupportLine(viewState.supporterBadge, onSupportClick)
        }
        Spacer(Modifier.height(16.dp))
      }
    }
  }
}

@Composable
private fun Modifier.periodSwipe(
  viewState: ListeningStatsViewState,
  onEarlierClick: () -> Unit,
  onLaterClick: () -> Unit,
): Modifier {
  val threshold = with(LocalDensity.current) { 64.dp.toPx() }
  val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
  return pointerInput(viewState.canGoBack, viewState.canGoForward, rtl) {
    var total = 0F
    detectHorizontalDragGestures(
      onDragStart = { total = 0F },
      onDragEnd = {
        // like turning back a page, the earlier period comes from the start side
        val towardsEnd = if (rtl) -total else total
        if (towardsEnd > threshold && viewState.canGoBack) onEarlierClick()
        if (towardsEnd < -threshold && viewState.canGoForward) onLaterClick()
      },
    ) { _, dragAmount ->
      total += dragAmount
    }
  }
}

@Composable
private fun KindChips(
  selected: PeriodKind,
  onKindClick: (PeriodKind) -> Unit,
) {
  Row(
    Modifier.horizontalScroll(rememberScrollState()),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    PeriodKind.entries.forEach { kind ->
      FilterChip(
        selected = kind == selected,
        onClick = { onKindClick(kind) },
        label = { Text(kind.label()) },
      )
    }
  }
}

@Composable
private fun Hero(
  viewState: ListeningStatsViewState,
  onEarlierClick: () -> Unit,
  onLaterClick: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  Surface(
    shape = MaterialTheme.shapes.extraLargeIncreased,
    // the primary is a light tone in the dark theme, which glares as a whole tile
    color = if (colors.surface.luminance() < 0.5F) colors.primaryContainer else colors.primary,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(start = 24.dp, end = 12.dp, top = 12.dp, bottom = 20.dp)) {
      Row(Modifier.heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = periodText(viewState.period, viewState.current),
          style = MaterialTheme.typography.titleMedium,
          modifier = Modifier.weight(1F),
        )
        if (viewState.period.kind != PeriodKind.AllTime) {
          val colors =
            IconButtonDefaults.iconButtonColors(
              disabledContentColor = LocalContentColor.current.copy(alpha = 0.38F),
            )
          IconButton(onClick = onEarlierClick, enabled = viewState.canGoBack, colors = colors) {
            Icon(
              painterResource(UiR.drawable.ic_chevron_left),
              contentDescription = stringResource(StringsR.string.listening_stats_earlier),
            )
          }
          IconButton(onClick = onLaterClick, enabled = viewState.canGoForward, colors = colors) {
            Icon(
              painterResource(UiR.drawable.ic_chevron_right),
              contentDescription = stringResource(StringsR.string.listening_stats_later),
            )
          }
        }
      }
      Column(Modifier.padding(end = 12.dp)) {
        if (viewState.empty) {
          Text(
            text = stringResource(StringsR.string.listening_stats_empty),
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(vertical = 40.dp),
          )
        } else {
          ListenedTime(viewState.listened)
          val quiet = LocalContentColor.current.copy(alpha = 0.8F)
          Text(
            text = daysText(viewState),
            style = MaterialTheme.typography.titleMedium,
            color = quiet,
          )
          if (viewState.countsSince != null) {
            Spacer(Modifier.height(4.dp))
            Text(
              text = countsSinceText(viewState.countsSince),
              style = MaterialTheme.typography.bodyMedium,
              color = quiet,
            )
          }
          if (viewState.bars.isNotEmpty()) {
            Spacer(Modifier.height(20.dp))
            Bars(
              bars = viewState.bars,
              labels = barLabels(viewState.period),
              color = { LocalContentColor.current },
              modifier = Modifier.height(112.dp),
            )
          }
        }
      }
    }
  }
}

@Composable
private fun daysText(viewState: ListeningStatsViewState): String =
  when (viewState.period.kind) {
    PeriodKind.Week,
    PeriodKind.Month ->
      pluralStringResource(
        StringsR.plurals.listening_stats_days_of,
        viewState.period.days,
        viewState.days,
        viewState.period.days,
      )
    PeriodKind.Year,
    PeriodKind.AllTime ->
      pluralStringResource(StringsR.plurals.listening_stats_days, viewState.days, viewState.days)
  }

@Composable
private fun ListenedTime(listened: Duration) {
  val minutes = listened.inWholeMinutes.toFloat()
  val animate = animationsEnabled()
  val animated = remember { Animatable(if (animate) 0F else minutes) }
  LaunchedEffect(minutes) {
    animated.animateTo(minutes, tween(durationMillis = 900))
  }
  Text(
    text = listenedText(animated.value.toLong().minutes),
    style = MaterialTheme.typography.displayLargeEmphasized.copy(fontFamily = FontFamily.Default),
    maxLines = 1,
    autoSize =
      TextAutoSize.StepBased(
        minFontSize = 28.sp,
        maxFontSize = MaterialTheme.typography.displayLargeEmphasized.fontSize,
      ),
  )
}

@Composable
internal fun Bars(
  bars: List<Duration>,
  labels: List<String>,
  color: @Composable (Int) -> Color,
  modifier: Modifier = Modifier,
) {
  val max = bars.max().coerceAtLeast(1.minutes)
  val animate = animationsEnabled()
  Row(
    modifier.fillMaxWidth().clearAndSetSemantics {},
    horizontalArrangement = Arrangement.spacedBy(if (bars.size > 12) 2.dp else 8.dp),
    verticalAlignment = Alignment.Bottom,
  ) {
    bars.forEachIndexed { index, bar ->
      val target = (bar / max).toFloat()
      val height = remember(bars.size, index) { Animatable(if (animate) 0F else target) }
      LaunchedEffect(height, target) {
        if (height.value == 0F && animate) delay(index * 40L)
        height.animateTo(
          target,
          spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        )
      }
      Column(
        Modifier.weight(1F).fillMaxHeight(),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        Box(Modifier.weight(1F).fillMaxWidth(), contentAlignment = Alignment.BottomCenter) {
          Box(
            Modifier.fillMaxWidth()
              .fillMaxHeight(height.value.coerceIn(0.04F, 1F))
              .clip(MaterialTheme.shapes.extraSmall)
              .background(
                if (bar > Duration.ZERO) color(index)
                else LocalContentColor.current.copy(alpha = 0.16F),
              ),
          )
        }
        Spacer(Modifier.height(4.dp))
        Text(
          text = labels[index],
          style = MaterialTheme.typography.labelSmall,
          color = LocalContentColor.current.copy(alpha = 0.8F),
          maxLines = 1,
          softWrap = false,
          // a day like 29 is wider than the bar of a month
          modifier = Modifier.wrapContentWidth(unbounded = true),
        )
      }
    }
  }
}

@Composable
private fun Shelf(
  finished: List<FinishedBookViewState>,
  kind: PeriodKind,
  onBookClick: (BookId) -> Unit,
) {
  Surface(
    shape = MaterialTheme.shapes.extraLargeIncreased,
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Column(Modifier.padding(vertical = 20.dp)) {
      Row(Modifier.padding(horizontal = 24.dp), verticalAlignment = Alignment.CenterVertically) {
        Text(
          text = stringResource(StringsR.string.listening_stats_books_finished),
          style = MaterialTheme.typography.titleLargeEmphasized,
          modifier = Modifier.weight(1F).semantics { heading() },
        )
        Text(
          text = countText(finished.size),
          style = MaterialTheme.typography.titleMedium,
          color = MaterialTheme.colorScheme.onPrimary,
          modifier =
            Modifier.padding(start = 16.dp)
              .background(MaterialTheme.colorScheme.primary, CircleShape)
              .padding(horizontal = 14.dp, vertical = 4.dp),
        )
      }
      if (finished.isNotEmpty()) {
        Spacer(Modifier.height(16.dp))
        Row(
          Modifier.horizontalScroll(rememberScrollState()).padding(horizontal = 24.dp),
          horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
          finished.forEachIndexed { index, book ->
            key(book.book.id) {
              ShelfBook(book, kind, index, onBookClick)
            }
          }
        }
      }
    }
  }
}

@Composable
private fun ShelfBook(
  finished: FinishedBookViewState,
  kind: PeriodKind,
  index: Int,
  onBookClick: (BookId) -> Unit,
) {
  val book = finished.book
  val animate = animationsEnabled()
  val landing = remember { Animatable(if (animate) 0F else 1F) }
  LaunchedEffect(landing) {
    if (animate) delay(300L + index * 120L)
    landing.animateTo(
      1F,
      spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )
  }
  // large text needs room for more of the name. At 96 dp, a phone cuts the next cover, which shows
  // that the shelf scrolls.
  val width = if (LocalDensity.current.fontScale >= 1.5F) 136.dp else 96.dp
  Column(
    Modifier.width(width)
      .clip(MaterialTheme.shapes.small)
      .then(
        if (book.inLibrary) {
          Modifier.clickable(role = Role.Button) { onBookClick(book.id) }
        } else {
          Modifier
        },
      ),
  ) {
    BookCover(
      book,
      Modifier.size(width).graphicsLayer {
        val progress = landing.value
        translationY = (1F - progress) * -60F
        scaleX = 0.6F + 0.4F * progress
        scaleY = 0.6F + 0.4F * progress
        alpha = progress.coerceIn(0F, 1F)
      },
    )
    Spacer(Modifier.height(8.dp))
    Text(
      text = book.name,
      style = MaterialTheme.typography.titleSmall.bookText(),
      maxLines = 2,
      overflow = TextOverflow.Ellipsis,
    )
    Text(
      text = finishedDayText(finished.day, kind),
      style = MaterialTheme.typography.bodySmall,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
  }
}

@Composable
internal fun BookCover(
  book: StatsBookViewState,
  modifier: Modifier = Modifier,
) {
  if (book.inLibrary) {
    AsyncImage(
      model = rememberCoverThumbnailRequest(book.cover),
      placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
      error = painterResource(UiR.drawable.album_art),
      contentScale = ContentScale.Crop,
      contentDescription = null,
      modifier = modifier.clip(CoverShape),
    )
  } else {
    Box(modifier.clip(CoverShape).background(MaterialTheme.colorScheme.outlineVariant))
  }
}

@Composable
private fun SupportLine(
  badge: SupporterBadge?,
  onClick: () -> Unit,
) {
  Surface(
    onClick = onClick,
    shape = MaterialTheme.shapes.largeIncreased,
    color = MaterialTheme.colorScheme.surfaceContainerLow,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
      if (badge != null) {
        SupporterBadgeIcon(badge = badge, size = 40.dp, contentDescription = badge.label())
      } else {
        Box(
          Modifier.size(40.dp)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest, CircleShape),
          contentAlignment = Alignment.Center,
        ) {
          Icon(
            painterResource(UiR.drawable.ic_favorite),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.tertiary,
            modifier = Modifier.size(20.dp),
          )
        }
      }
      Column(Modifier.padding(horizontal = 16.dp).weight(1F)) {
        if (badge != null) {
          Text(
            text = stringResource(StringsR.string.settings_support_supporter_title),
            style = MaterialTheme.typography.titleSmall,
          )
        } else {
          Text(
            text = stringResource(StringsR.string.settings_support_support_voice_title),
            style = MaterialTheme.typography.titleSmall,
          )
          Text(
            text = stringResource(StringsR.string.settings_support_support_voice_summary),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
      }
      Icon(
        painterResource(UiR.drawable.ic_chevron_right),
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

private val previewToday = LocalDate.of(2026, 11, 20)

private val previewWeek =
  ListeningStatsViewState(
    period = PeriodKind.Week.period(previewToday, back = 0, DayOfWeek.MONDAY),
    current = true,
    canGoBack = true,
    canGoForward = false,
    listened = 6.hours + 40.minutes,
    days = 5,
    bars = listOf(60, 95, 0, 120, 45, 80, 0).map { it.minutes },
    countsSince = null,
    name = ListenerName.NightOwl(percent = 61),
    wideFact =
      WideFact.WhenYouListen(
        List(24) { hour ->
          when (hour) {
            in 22..23 -> 70.minutes
            0 -> 55.minutes
            7 -> 35.minutes
            in 17..19 -> 40.minutes
            else -> Duration.ZERO
          }
        },
      ),
    smallFacts = listOf(SmallFact.Change(80.minutes), SmallFact.Streak(current = 4, longest = 9)),
    finished =
      listOf(
        FinishedBookViewState(
          StatsBookViewState(BookId("1"), "Long Way North", null, inLibrary = true),
          previewToday.minusDays(1),
        ),
        FinishedBookViewState(
          StatsBookViewState(
            BookId("2"),
            "The Unlikely Pilgrimage of the Lighthouse Keeper",
            null,
            inLibrary = true,
          ),
          previewToday.minusDays(2),
        ),
        FinishedBookViewState(
          StatsBookViewState(BookId("3"), "Paper Moons", null, inLibrary = false),
          previewToday.minusDays(3),
        ),
      ),
    supporterBadge = null,
  )

@Composable
@Preview(heightDp = 1400)
@Preview(heightDp = 1400, uiMode = Configuration.UI_MODE_NIGHT_YES, name = "dark")
@Preview(heightDp = 2500, fontScale = 2F, name = "large text")
private fun ListeningStatsWeekPreview() {
  StatsPreview(previewWeek)
}

@Composable
@Preview(heightDp = 1150)
private fun ListeningStatsMonthPreview() {
  StatsPreview(
    previewWeek.copy(
      period = PeriodKind.Month.period(previewToday, back = 1, DayOfWeek.MONDAY),
      current = false,
      canGoForward = true,
      listened = 71.hours + 5.minutes,
      days = 28,
      bars = List(31) { (it * 37 % 190).minutes },
      name = ListenerName.Audiomaniac(perDay = 2.hours + 17.minutes),
      wideFact =
        WideFact.MostListened(
          StatsBookViewState(
            BookId("2"),
            "The Unlikely Pilgrimage of the Lighthouse Keeper",
            null,
            inLibrary = true,
          ),
          listened = 31.hours + 20.minutes,
        ),
      smallFacts =
        listOf(
          SmallFact.LongestDay(5.hours + 10.minutes, LocalDate.of(2026, 10, 14)),
          SmallFact.YourDay(DayOfWeek.WEDNESDAY),
        ),
      finished = previewWeek.finished.take(1),
    ),
  )
}

@Composable
@Preview(heightDp = 1050)
private fun ListeningStatsYearPreview() {
  StatsPreview(
    previewWeek.copy(
      period = PeriodKind.Year.period(previewToday, back = 0, DayOfWeek.MONDAY),
      listened = 212.hours + 30.minutes,
      days = 143,
      bars = listOf(0, 0, 0, 0, 0, 0, 0, 0, 0, 62, 97, 53).map { it.hours },
      name = ListenerName.Juggler(books = 4),
      wideFact =
        WideFact.Forecast(
          StatsBookViewState(BookId("1"), "Long Way North", null, inLibrary = true),
          left = 9.hours,
          day = previewToday.plusDays(4),
          today = previewToday,
        ),
      smallFacts =
        listOf(
          SmallFact.LongestMonth(97.hours, Month.NOVEMBER),
          SmallFact.Saved(saved = 43.hours + 10.minutes, speed = 1.25F),
        ),
      finished = emptyList(),
    ),
  )
}

@Composable
@Preview
private fun ListeningStatsEmptyPreview() {
  StatsPreview(
    previewWeek.copy(
      period = PeriodKind.Month.period(previewToday, back = 1, DayOfWeek.MONDAY),
      current = false,
      canGoBack = false,
      canGoForward = true,
      listened = Duration.ZERO,
      days = 0,
      bars = List(31) { Duration.ZERO },
      name = null,
      wideFact = null,
      smallFacts = emptyList(),
      finished = emptyList(),
    ),
  )
}

@Composable
@Preview(heightDp = 900)
private fun ListeningStatsAllTimeSupporterPreview() {
  StatsPreview(
    previewWeek.copy(
      period = PeriodKind.AllTime.period(previewToday, back = 0, DayOfWeek.MONDAY),
      canGoBack = false,
      listened = 52.hours + 5.minutes,
      days = 31,
      bars = emptyList(),
      countsSince = LocalDate.of(2026, 10, 8),
      name = ListenerName.PartOfDay(DayPart.Evening, percent = 52),
      wideFact = null,
      smallFacts = listOf(SmallFact.LongestStretch(2.hours + 15.minutes)),
      finished = emptyList(),
      supporterBadge = SupporterBadge.ThreeMonths,
    ),
  )
}

@Composable
private fun StatsPreview(viewState: ListeningStatsViewState) {
  VoiceTheme {
    ListeningStatsScreen(
      viewState = viewState,
      onCloseClick = {},
      onKindClick = {},
      onEarlierClick = {},
      onLaterClick = {},
      onBookClick = {},
      onSupportClick = {},
    )
  }
}
