@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.series

import androidx.annotation.PluralsRes
import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.launch
import voice.core.common.rootGraphAs
import voice.core.data.BookCategory
import voice.core.strings.R as StringsR
import voice.core.ui.CoverTheme
import voice.core.ui.EntranceState
import voice.core.ui.VoiceTheme
import voice.core.ui.bookText
import voice.core.ui.entrance
import voice.core.ui.rememberEntranceState
import voice.core.ui.segmentedShape
import voice.features.bookOverview.bottomSheet.ActionRow
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.style
import voice.features.bookOverview.overview.SeriesStackViewState
import voice.features.bookOverview.views.CoverStack
import voice.features.bookOverview.views.seriesCounts
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.OverlayNav

@BindingContainer
@ContributesTo(AppScope::class)
object SeriesActionsProvider {

  @Provides
  @IntoSet
  fun seriesActionsNavEntryProvider(): NavEntryProvider<*> =
    NavEntryProvider<Destination.SeriesActions> { key ->
      NavEntry(key, metadata = OverlayNav.overlay()) {
        SeriesActionsScreen(key.name)
      }
    }
}

@Composable
private fun SeriesActionsScreen(name: String) {
  val viewModel =
    retain(name) {
      rootGraphAs<SeriesActionsViewModel.Factory.Provider>()
        .seriesActionsViewModelFactory
        .create(name)
    }
  // shown once the series is loaded, so the sheet rises at its full height
  val state = viewModel.state() ?: return
  CoverTheme(cover = state.series.covers.first()) {
    val sheetState =
      rememberBottomSheetState(
        initialValue = Hidden,
        enabledValues = setOf(Hidden, Expanded),
      )
    val scope = rememberCoroutineScope()
    // the first choice wins, so taps while the sheet slides away can't add a second action
    var closing by remember { mutableStateOf(false) }
    ModalBottomSheet(
      sheetState = sheetState,
      onDismissRequest = {
        if (!closing) viewModel.onClose()
      },
    ) {
      SeriesActionsContent(
        state = state,
        onItemClick = { item ->
          if (!closing) {
            closing = true
            scope.launch {
              // a second tap or a back press stops the slide, the choice still counts
              try {
                sheetState.hide()
              } finally {
                viewModel.onItemClick(item)
                viewModel.onClose()
              }
            }
          }
        },
      )
    }
  }
}

@Composable
private fun SeriesActionsContent(
  state: SeriesActionsViewState,
  onItemClick: (BottomSheetItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val entrance = rememberEntranceState()
  var confirming by rememberSaveable { mutableStateOf<BottomSheetItem?>(null) }
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
  ) {
    SeriesHeader(
      series = state.series,
      modifier = Modifier.entrance(entrance, 0).padding(horizontal = 8.dp),
    )
    Spacer(Modifier.height(24.dp))
    AnimatedContent(
      // keyed by the item, so new counts don't swap the step again
      targetState = state.statuses.find { it.item == confirming },
      contentKey = { it?.item },
      transitionSpec = {
        val enter =
          fadeIn(tween(durationMillis = 220, delayMillis = 90)) +
            scaleIn(tween(durationMillis = 220, delayMillis = 90), initialScale = 0.92F)
        (enter togetherWith fadeOut(tween(durationMillis = 90))).using(
          SizeTransform(clip = false),
        )
      },
      label = "seriesActionsStep",
    ) { confirm ->
      if (confirm == null) {
        SeriesActionRows(
          state = state,
          entrance = entrance,
          onStatusClick = { status ->
            if (status.resets > 0) confirming = status.item else onItemClick(status.item)
          },
          onItemClick = onItemClick,
        )
      } else {
        ConfirmStatus(
          status = confirm,
          onConfirm = { onItemClick(confirm.item) },
          onCancel = { confirming = null },
        )
      }
    }
  }
}

@Composable
private fun SeriesActionRows(
  state: SeriesActionsViewState,
  entrance: EntranceState,
  onStatusClick: (SeriesActionsViewState.Status) -> Unit,
  onItemClick: (BottomSheetItem) -> Unit,
) {
  Column {
    if (state.statuses.isNotEmpty()) {
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        state.statuses.forEachIndexed { index, status ->
          ActionRow(
            item = status.item,
            title = stringResource(status.item.statusTexts.title),
            style = status.item.style(),
            shape = segmentedShape(index, state.statuses.size),
            onClick = { onStatusClick(status) },
            modifier = Modifier.entrance(entrance, 1 + index),
          )
        }
      }
      Spacer(Modifier.height(20.dp))
    }
    val visibility = if (state.series.hidden) BottomSheetItem.Show else BottomSheetItem.Hide
    ActionRow(
      item = visibility,
      style = visibility.style(),
      shape = RoundedCornerShape(24.dp),
      onClick = { onItemClick(visibility) },
      modifier = Modifier.entrance(entrance, 1 + state.statuses.size),
    )
  }
}

@Composable
private fun ConfirmStatus(
  status: SeriesActionsViewState.Status,
  onConfirm: () -> Unit,
  onCancel: () -> Unit,
) {
  val texts = status.item.statusTexts
  Column(Modifier.fillMaxWidth()) {
    Text(
      text = pluralStringResource(texts.confirm, status.resets, status.resets),
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.padding(horizontal = 8.dp),
    )
    Spacer(Modifier.height(24.dp))
    // stacked, as the names of the statuses are too long to share a row in many languages
    Button(
      onClick = onConfirm,
      shapes = ButtonDefaults.shapes(),
      modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
    ) {
      Icon(
        imageVector = status.item.icon,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(stringResource(texts.title))
    }
    Spacer(Modifier.height(8.dp))
    OutlinedButton(
      onClick = onCancel,
      shapes = ButtonDefaults.shapes(),
      modifier = Modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
    ) {
      Text(stringResource(StringsR.string.common_dialog_cancel))
    }
  }
}

private class StatusTexts(
  @StringRes val title: Int,
  @PluralsRes val confirm: Int,
)

// the words of a book's status don't fit a series in every language
private val BottomSheetItem.statusTexts: StatusTexts
  get() =
    if (this == BottomSheetItem.BookCategoryMarkAsCompleted) {
      StatusTexts(
        title = StringsR.string.series_mark_completed,
        confirm = StringsR.plurals.series_mark_completed_confirm,
      )
    } else {
      StatusTexts(
        title = StringsR.string.series_mark_not_started,
        confirm = StringsR.plurals.series_mark_not_started_confirm,
      )
    }

@Composable
private fun SeriesHeader(
  series: SeriesStackViewState,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    // not the series' shared cover, the library's stack stays where it is under the sheet
    CoverStack(
      covers = series.covers,
      gapColor = BottomSheetDefaults.ContainerColor,
      modifier = Modifier.size(88.dp),
    )
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1F)) {
      Text(
        text = series.name,
        style = MaterialTheme.typography.titleLargeEmphasized.bookText(),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      if (series.author != null) {
        Text(
          text = series.author,
          style = MaterialTheme.typography.bodyMedium.bookText(),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        text = seriesCounts(series.bookCount, series.finishedCount),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
@Preview
private fun SeriesActionsContentPreview() {
  VoiceTheme {
    Surface(color = BottomSheetDefaults.ContainerColor) {
      SeriesActionsContent(
        state =
          SeriesActionsViewState(
            series =
              SeriesStackViewState(
                key = "thesignalsaga",
                name = "The Signal Saga",
                author = "Daniel Hartwell",
                covers = listOf(null, null, null),
                bookCount = 4,
                finishedCount = 1,
                progress = listOf(1F, 0.4F, 0F, 0F),
                category = BookCategory.CURRENT,
              ),
            statuses =
              listOf(
                SeriesActionsViewState.Status(
                  BottomSheetItem.BookCategoryMarkAsNotStarted,
                  resets = 2,
                ),
                SeriesActionsViewState.Status(
                  BottomSheetItem.BookCategoryMarkAsCompleted,
                  resets = 1,
                ),
              ),
          ),
        onItemClick = {},
      )
    }
  }
}
