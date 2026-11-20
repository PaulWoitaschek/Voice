@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.stats

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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import coil3.compose.AsyncImage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import java.time.DayOfWeek
import java.time.LocalDate
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
      ) {
        KindChips(viewState.period.kind, onKindClick)
        if (viewState.period.kind != PeriodKind.AllTime) {
          PeriodRow(viewState, onEarlierClick, onLaterClick)
        } else {
          Spacer(Modifier.height(16.dp))
        }
        if (viewState.empty) {
          Text(
            text = stringResource(StringsR.string.listening_stats_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 48.dp),
          )
        } else {
          Stats(viewState, onBookClick)
          Spacer(Modifier.height(32.dp))
          SupportLine(viewState.supporterBadge, onSupportClick)
          Spacer(Modifier.height(24.dp))
        }
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
private fun PeriodRow(
  viewState: ListeningStatsViewState,
  onEarlierClick: () -> Unit,
  onLaterClick: () -> Unit,
) {
  Row(Modifier.padding(vertical = 8.dp), verticalAlignment = Alignment.CenterVertically) {
    IconButton(onClick = onEarlierClick, enabled = viewState.canGoBack) {
      Icon(
        painterResource(UiR.drawable.ic_chevron_left),
        contentDescription = stringResource(StringsR.string.listening_stats_earlier),
      )
    }
    Text(
      text = periodText(viewState.period, viewState.current),
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
      modifier = Modifier.weight(1F),
    )
    IconButton(onClick = onLaterClick, enabled = viewState.canGoForward) {
      Icon(
        painterResource(UiR.drawable.ic_chevron_right),
        contentDescription = stringResource(StringsR.string.listening_stats_later),
      )
    }
  }
}

@Composable
private fun Stats(
  viewState: ListeningStatsViewState,
  onBookClick: (BookId) -> Unit,
) {
  Column {
    val kind = viewState.period.kind
    ListenedTime(viewState.listened)
    Text(
      text =
        when (kind) {
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
            pluralStringResource(
              StringsR.plurals.listening_stats_days,
              viewState.days,
              viewState.days,
            )
        },
      style = MaterialTheme.typography.titleMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    if (viewState.countsSince != null) {
      Spacer(Modifier.height(4.dp))
      Text(
        text = countsSinceText(viewState.countsSince),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
    if (viewState.bars.isNotEmpty()) {
      Spacer(Modifier.height(24.dp))
      Bars(viewState.bars, barLabels(viewState.period))
    }
    Spacer(Modifier.height(24.dp))
    Tiles(viewState.finished.size, viewState.saved)
    if (viewState.finished.isNotEmpty()) {
      Spacer(Modifier.height(24.dp))
      Text(
        text = stringResource(StringsR.string.listening_stats_finished),
        style = MaterialTheme.typography.titleLarge,
        modifier = Modifier.semantics { heading() },
      )
      Spacer(Modifier.height(8.dp))
      viewState.finished.forEachIndexed { index, book ->
        key(book.id) {
          FinishedRow(book, kind, index, onBookClick)
        }
      }
    }
  }
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
    style = MaterialTheme.typography.displayMediumEmphasized.copy(fontFamily = FontFamily.Default),
  )
}

@Composable
private fun Bars(
  bars: List<Duration>,
  labels: List<String>,
) {
  val max = bars.max().coerceAtLeast(1.minutes)
  val animate = animationsEnabled()
  Row(
    Modifier.fillMaxWidth().height(140.dp).clearAndSetSemantics {},
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
              .fillMaxHeight(height.value.coerceIn(if (bar > Duration.ZERO) 0.03F else 0.01F, 1F))
              .clip(MaterialTheme.shapes.extraSmall)
              .background(
                if (bar > Duration.ZERO) {
                  MaterialTheme.colorScheme.primary
                } else {
                  MaterialTheme.colorScheme.surfaceContainerHighest
                },
              ),
          )
        }
        Spacer(Modifier.height(4.dp))
        Text(
          text = labels[index],
          style = MaterialTheme.typography.labelSmall,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
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
private fun Tiles(
  finishedCount: Int,
  saved: Duration?,
) {
  val finished = stringResource(StringsR.string.listening_stats_books_finished)
  val savedLabel = stringResource(StringsR.string.listening_stats_saved)
  val savedText = saved?.let { listenedText(it) }
  if (LocalDensity.current.fontScale >= 1.5F) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
      Tile(finishedCount.toString(), finished, Modifier.fillMaxWidth())
      if (savedText != null) Tile(savedText, savedLabel, Modifier.fillMaxWidth())
    }
  } else {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
      Tile(finishedCount.toString(), finished, Modifier.weight(1F))
      if (savedText != null) Tile(savedText, savedLabel, Modifier.weight(1F))
    }
  }
}

@Composable
private fun Tile(
  value: String,
  label: String,
  modifier: Modifier = Modifier,
) {
  Surface(
    modifier = modifier,
    shape = MaterialTheme.shapes.largeIncreased,
    color = MaterialTheme.colorScheme.secondaryContainer,
  ) {
    Column(Modifier.padding(16.dp)) {
      Text(
        value,
        style =
          MaterialTheme.typography.headlineSmallEmphasized.copy(fontFamily = FontFamily.Default),
      )
      Text(label, style = MaterialTheme.typography.bodyMedium)
    }
  }
}

@Composable
private fun FinishedRow(
  book: FinishedBookViewState,
  kind: PeriodKind,
  index: Int,
  onBookClick: (BookId) -> Unit,
) {
  val animate = animationsEnabled()
  val landing = remember { Animatable(if (animate) 0F else 1F) }
  LaunchedEffect(landing) {
    if (animate) delay(300L + index * 120L)
    landing.animateTo(
      1F,
      spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
    )
  }
  Row(
    Modifier.fillMaxWidth()
      .clip(MaterialTheme.shapes.largeIncreased)
      .then(if (book.inLibrary) Modifier.clickable { onBookClick(book.id) } else Modifier)
      .padding(vertical = 6.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    val cover =
      Modifier.size(56.dp).graphicsLayer {
        val progress = landing.value
        translationY = (1F - progress) * -60F
        scaleX = 0.6F + 0.4F * progress
        scaleY = 0.6F + 0.4F * progress
        alpha = progress.coerceIn(0F, 1F)
      }
    if (book.inLibrary) {
      AsyncImage(
        model = rememberCoverThumbnailRequest(book.cover),
        placeholder = ColorPainter(MaterialTheme.colorScheme.surfaceContainerHighest),
        error = painterResource(UiR.drawable.album_art),
        contentScale = ContentScale.Crop,
        contentDescription = null,
        modifier = cover.clip(CoverShape),
      )
    } else {
      Box(cover)
    }
    Column(Modifier.padding(start = 16.dp).weight(1F)) {
      Text(
        text = book.name,
        style = MaterialTheme.typography.titleMedium.bookText(),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = listOfNotNull(book.author, finishedDayText(book.day, kind)).joinToString(" · "),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
    }
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

@Composable
@PreviewLightDark
@Preview(fontScale = 2F, name = "large text")
private fun ListeningStatsWeekPreview() {
  val monday = LocalDate.of(2026, 11, 16)
  StatsPreview(
    ListeningStatsViewState(
      period = PeriodKind.Week.period(monday.plusDays(4), back = 0, DayOfWeek.MONDAY),
      current = true,
      canGoBack = true,
      canGoForward = false,
      listened = 6.hours + 40.minutes,
      days = 5,
      bars = listOf(60, 95, 0, 120, 45, 80, 0).map { it.minutes },
      saved = 70.minutes,
      countsSince = null,
      finished =
        listOf(
          FinishedBookViewState(
            BookId("1"),
            "Long Way North",
            "Daniel Hartwell",
            null,
            monday.plusDays(3),
            inLibrary = true,
          ),
          FinishedBookViewState(
            BookId("2"),
            "Paper Moons",
            "Lena Morgan",
            null,
            monday.plusDays(1),
            inLibrary = false,
          ),
        ),
      supporterBadge = null,
    ),
  )
}

@Composable
@Preview
private fun ListeningStatsMonthPreview() {
  StatsPreview(
    ListeningStatsViewState(
      period = PeriodKind.Month.period(LocalDate.of(2026, 10, 20), back = 0, DayOfWeek.MONDAY),
      current = true,
      canGoBack = true,
      canGoForward = false,
      listened = 21.hours + 5.minutes,
      days = 18,
      bars = List(31) { (it * 37 % 90).minutes },
      saved = null,
      countsSince = null,
      finished = emptyList(),
      supporterBadge = null,
    ),
  )
}

@Composable
@Preview
private fun ListeningStatsEmptyPreview() {
  StatsPreview(
    ListeningStatsViewState(
      period = PeriodKind.Month.period(LocalDate.of(2026, 11, 20), back = 1, DayOfWeek.MONDAY),
      current = false,
      canGoBack = false,
      canGoForward = true,
      listened = Duration.ZERO,
      days = 0,
      bars = List(31) { Duration.ZERO },
      saved = null,
      countsSince = null,
      finished = emptyList(),
      supporterBadge = null,
    ),
  )
}

@Composable
@Preview
private fun ListeningStatsAllTimeSupporterPreview() {
  StatsPreview(
    ListeningStatsViewState(
      period = PeriodKind.AllTime.period(LocalDate.of(2026, 11, 20), back = 0, DayOfWeek.MONDAY),
      current = true,
      canGoBack = false,
      canGoForward = false,
      listened = 52.hours + 5.minutes,
      days = 31,
      bars = emptyList(),
      saved = null,
      countsSince = LocalDate.of(2026, 10, 8),
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
