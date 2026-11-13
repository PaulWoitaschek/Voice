@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.author

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.BottomSheetDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import voice.core.ui.CoverTheme
import voice.core.ui.VoiceTheme
import voice.core.ui.entrance
import voice.core.ui.rememberEntranceState
import voice.features.bookOverview.bottomSheet.ActionRow
import voice.features.bookOverview.bottomSheet.BottomSheetItem
import voice.features.bookOverview.bottomSheet.style
import voice.features.bookOverview.overview.AuthorEntryViewState
import voice.features.bookOverview.views.AuthorCovers
import voice.features.bookOverview.views.authorCounts
import voice.features.bookOverview.views.authorName
import voice.features.bookOverview.views.nameText
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.OverlayNav

@BindingContainer
@ContributesTo(AppScope::class)
object AuthorActionsProvider {

  @Provides
  @IntoSet
  fun authorActionsNavEntryProvider(): NavEntryProvider<*> =
    NavEntryProvider<Destination.AuthorActions> { key ->
      NavEntry(key, metadata = OverlayNav.overlay()) {
        AuthorActionsScreen(key)
      }
    }
}

@Composable
private fun AuthorActionsScreen(destination: Destination.AuthorActions) {
  val viewModel =
    retain(destination) {
      rootGraphAs<AuthorActionsViewModel.Factory.Provider>()
        .authorActionsViewModelFactory
        .create(destination)
    }
  // shown once the books are loaded, so the sheet rises at its full height
  val author = viewModel.state() ?: return
  CoverTheme(cover = author.covers.first()) {
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
      AuthorActionsContent(
        author = author,
        onSelect = {
          if (!closing) {
            closing = true
            scope.launch {
              // a second tap or a back press stops the slide, the choice still counts
              try {
                sheetState.hide()
              } finally {
                viewModel.onSelect()
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
private fun AuthorActionsContent(
  author: AuthorEntryViewState,
  onSelect: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val entrance = rememberEntranceState()
  Column(
    modifier =
      modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 16.dp)
        .padding(bottom = 24.dp),
  ) {
    AuthorHeader(
      author = author,
      modifier = Modifier.entrance(entrance, 0).padding(horizontal = 8.dp),
    )
    Spacer(Modifier.height(24.dp))
    ActionRow(
      item = BottomSheetItem.Select,
      style = BottomSheetItem.Select.style(),
      shape = MaterialTheme.shapes.largeIncreased,
      onClick = onSelect,
      modifier = Modifier.entrance(entrance, 1),
    )
  }
}

@Composable
private fun AuthorHeader(
  author: AuthorEntryViewState,
  modifier: Modifier = Modifier,
) {
  Row(
    modifier = modifier,
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AuthorCovers(covers = author.covers, modifier = Modifier.size(88.dp))
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1F)) {
      Text(
        text = authorName(author),
        style = MaterialTheme.typography.titleLargeEmphasized.nameText(author.name),
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
      )
      Spacer(Modifier.height(4.dp))
      Text(
        text = authorCounts(author.seriesCount, author.bookCount),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
@Preview
private fun AuthorActionsContentPreview() {
  VoiceTheme {
    Surface(color = BottomSheetDefaults.ContainerColor) {
      AuthorActionsContent(
        author =
          AuthorEntryViewState(
            key = "lenamorgan",
            name = "Lena Morgan",
            covers = listOf(null, null, null, null),
            seriesCount = 1,
            bookCount = 4,
            category = BookCategory.NOT_STARTED,
          ),
        onSelect = {},
      )
    }
  }
}
