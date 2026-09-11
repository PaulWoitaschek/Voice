package voice.features.listeningStats

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface ListeningStatsGraph {
  val listeningStatsViewModelFactory: ListeningStatsViewModel.Factory
}

@ContributesTo(AppScope::class)
interface ListeningStatsProvider {

  @Provides
  @IntoSet
  fun listeningStatsNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.ListeningStats> { key ->
    NavEntry(key) {
      ListeningStatsScreen(bookId = key.bookId)
    }
  }
}

// The preview shows a fixed populated state for quickly checking layout and duration formatting.
@Composable
@Preview
private fun ListeningStatsPreview() {
  VoiceTheme {
    ListeningStatsScreen(
      viewState = ListeningStatsViewState.preview(),
      onClose = {},
      onPeriodClick = {},
    )
  }
}

// The entry screen creates the per-book ViewModel so long-pressing different books never shares state.
@Composable
fun ListeningStatsScreen(bookId: BookId) {
  val viewModel = retain(bookId.value) {
    rootGraphAs<ListeningStatsGraph>().listeningStatsViewModelFactory.create(bookId)
  }
  ListeningStatsScreen(
    viewState = viewModel.viewState(),
    onClose = viewModel::close,
    onPeriodClick = viewModel::selectPeriod,
  )
}

// The real page keeps a flat layout instead of wrapping the review in another visual card.
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ListeningStatsScreen(
  viewState: ListeningStatsViewState,
  onClose: () -> Unit,
  onPeriodClick: (ListeningStatsPeriod) -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { Text(text = stringResource(StringsR.string.listening_stats_title)) },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(
              imageVector = VoiceIcons.Close,
              contentDescription = stringResource(StringsR.string.common_action_close),
            )
          }
        },
      )
    },
  ) { contentPadding ->
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(contentPadding)
        .padding(16.dp),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Text(
        text = viewState.title,
        style = MaterialTheme.typography.headlineSmall,
      )
      PeriodRow(
        selectedPeriod = viewState.selectedPeriod,
        onPeriodClick = onPeriodClick,
      )
      TotalSection(viewState = viewState)
    }
  }
}

// Period switching sits at the top so the time window can be changed quickly in the per-book context.
@Composable
private fun PeriodRow(
  selectedPeriod: ListeningStatsPeriod,
  onPeriodClick: (ListeningStatsPeriod) -> Unit,
) {
  Row(
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    ListeningStatsPeriod.entries.forEach { period ->
      FilterChip(
        selected = period == selectedPeriod,
        onClick = { onPeriodClick(period) },
        label = { Text(text = stringResource(period.titleRes)) },
      )
    }
  }
}

// The total uses a full-width section for density without adding extra overlays.
@Composable
private fun TotalSection(viewState: ListeningStatsViewState) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Text(
      text = stringResource(StringsR.string.listening_stats_total_title),
      style = MaterialTheme.typography.titleMedium,
    )
    if (viewState.empty) {
      // The empty state only prompts to keep playing instead of dressing zero up as valid stats.
      EmptyStats()
    } else {
      DurationText(durationMs = viewState.durationMs)
    }
  }
}

// The empty state must say listening has not been recorded yet, so blank is not mistaken for a load failure.
@Composable
private fun EmptyStats() {
  Row(
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Icon(
      imageVector = VoiceIcons.History,
      contentDescription = null,
    )
    Text(
      text = stringResource(StringsR.string.listening_stats_empty),
      style = MaterialTheme.typography.bodyMedium,
    )
  }
}

// Duration text stops at minutes so second-level jitter does not break the review feel.
@Composable
private fun DurationText(durationMs: Long) {
  val duration = listeningStatsDuration(durationMs)
  Text(
    text = if (duration.hasHours) {
      stringResource(
        StringsR.string.listening_stats_duration_hours_minutes,
        duration.hours,
        duration.minutes,
      )
    } else {
      stringResource(StringsR.string.listening_stats_duration_minutes, duration.minutes)
    },
    style = MaterialTheme.typography.displaySmall,
  )
}
