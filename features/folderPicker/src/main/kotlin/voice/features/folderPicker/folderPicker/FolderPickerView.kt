@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.folderPicker.folderPicker

import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.folders.FolderType
import voice.core.ui.AuroraBackground
import voice.core.ui.EntranceState
import voice.core.ui.VoiceTheme
import voice.core.ui.entrance
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.plus
import voice.core.ui.rememberAnimationClock
import voice.core.ui.rememberEntranceState
import voice.core.ui.segmentedShape
import voice.features.folderPicker.addcontent.FolderHero
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface FolderPickerGraph {
  val folderPickerViewModel: FolderPickerViewModel
}

@BindingContainer
@ContributesTo(AppScope::class)
object FolderPickerProvider {

  @Provides
  @IntoSet
  fun folderPickerNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.FolderPicker> { key ->
    NavEntry(key) {
      FolderOverview()
    }
  }
}

@Composable
fun FolderOverview() {
  val viewModel: FolderPickerViewModel = retain<FolderPickerViewModel> {
    rootGraphAs<FolderPickerGraph>()
      .folderPickerViewModel
  }
  val viewState = viewModel.viewState()
  FolderOverviewView(
    viewState = viewState,
    onAddClick = {
      viewModel.add()
    },
    onDeleteClick = {
      viewModel.removeFolder(it)
    },
    onFolderClick = viewModel::changeType,
    onCloseClick = viewModel::onCloseClick,
  )
}

/**
 * The audiobook folders over the drifting aurora, grouped like the books on the review screen. Each
 * leads with a sticker for how its books are found. Without any, the bobbing folder from adding
 * books asks for the first one.
 */
@Composable
private fun FolderOverviewView(
  viewState: FolderPickerViewState,
  onAddClick: () -> Unit,
  onDeleteClick: (FolderPickerViewState.Item) -> Unit,
  onFolderClick: (FolderPickerViewState.Item) -> Unit,
  onCloseClick: () -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val clock = rememberAnimationClock(running = true)
  var entered by rememberSaveable { mutableStateOf(false) }
  // the folders float in once they are read
  val entrance = if (viewState.loading) null else rememberEntranceState(animate = !entered)
  LaunchedEffect(entrance) { if (entrance != null) entered = true }
  Box(Modifier.fillMaxSize()) {
    AuroraBackground(
      clock = { clock.value },
      showStars = false,
      modifier = Modifier.fillMaxSize(),
    )
    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      topBar = {
        FolderOverviewTopBar(
          scrollBehavior = scrollBehavior,
          onCloseClick = onCloseClick,
        )
      },
      floatingActionButton = {
        if (viewState.showActions) {
          val text = stringResource(StringsR.string.common_action_add)
          ExtendedFloatingActionButton(
            text = { Text(text) },
            // the text alone doesn't reach accessibility services
            icon = { Icon(VoiceIcons.Add, contentDescription = text) },
            onClick = onAddClick,
          )
        }
      },
    ) { contentPadding ->
      LazyColumn(
        modifier = Modifier.fillMaxSize(),
        // leaves room to scroll the last folder out from under the add button
        contentPadding = contentPadding + PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 96.dp),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
      ) {
        if (entrance != null) {
          folders(
            viewState = viewState,
            entrance = entrance,
            clock = { clock.value },
            onFolderClick = onFolderClick,
            onDeleteClick = onDeleteClick,
          )
        }
      }
    }
  }
}

private fun LazyListScope.folders(
  viewState: FolderPickerViewState,
  entrance: EntranceState,
  clock: () -> Float,
  onFolderClick: (FolderPickerViewState.Item) -> Unit,
  onDeleteClick: (FolderPickerViewState.Item) -> Unit,
) {
  val itemModifier = Modifier
    .widthIn(max = 640.dp)
    .fillMaxWidth()
  if (viewState.items.isEmpty()) {
    item(key = "empty") {
      EmptyFolders(
        clock = clock,
        modifier = itemModifier.entrance(entrance, 0),
      )
    }
    return
  }
  itemsIndexed(
    items = viewState.items,
    // the made up folders of the kiosk mode share an empty uri
    key = { _, item -> "${item.folderType}/${item.id}/${item.name}" },
  ) { index, item ->
    // a single file is always one book, a folder can change how its books are found
    val canChangeType = viewState.showActions && item.folderType != FolderType.SingleFile
    FolderRow(
      item = item,
      shape = segmentedShape(index, viewState.items.size),
      onClick = if (canChangeType) {
        { onFolderClick(item) }
      } else {
        null
      },
      onDeleteClick = if (viewState.showActions) {
        { onDeleteClick(item) }
      } else {
        null
      },
      // the entrance is over by the time later folders are scrolled to, so they share the last delay
      modifier = itemModifier
        .animateItem()
        .entrance(entrance, minOf(index, 6)),
    )
  }
}

@Composable
private fun EmptyFolders(
  clock: () -> Float,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.padding(start = 8.dp, end = 8.dp, top = 16.dp),
    horizontalAlignment = Alignment.CenterHorizontally,
  ) {
    FolderHero(
      clock = clock,
      modifier = Modifier
        .fillMaxWidth()
        .height(240.dp),
    )
    Spacer(Modifier.height(24.dp))
    Text(
      text = stringResource(StringsR.string.library_empty_add_first_book),
      style = MaterialTheme.typography.headlineSmallEmphasized,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
private fun FolderOverviewTopBar(
  scrollBehavior: TopAppBarScrollBehavior,
  onCloseClick: () -> Unit,
) {
  LargeFlexibleTopAppBar(
    title = {
      Text(stringResource(StringsR.string.library_folders_title))
    },
    subtitle = {
      Text(stringResource(StringsR.string.settings_library_folders_summary))
    },
    navigationIcon = {
      val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
      IconButton(onClick = onCloseClick) {
        Icon(
          modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1F else 1F },
          imageVector = VoiceIcons.ArrowBack,
          contentDescription = stringResource(StringsR.string.common_action_back),
        )
      }
    },
    colors = TopAppBarDefaults.topAppBarColors(
      containerColor = Color.Transparent,
      scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
    ),
    scrollBehavior = scrollBehavior,
  )
}

@Composable
@Preview
private fun FolderOverviewPreview() {
  VoiceTheme {
    FolderOverviewView(
      viewState = FolderPickerViewState(
        items = listOf(
          FolderPickerViewState.Item(
            name = "My Audiobooks",
            id = Uri.EMPTY,
            folderType = FolderType.Root,
          ),
          FolderPickerViewState.Item(
            name = "Brandon Sanderson",
            id = Uri.EMPTY,
            folderType = FolderType.Author,
          ),
          FolderPickerViewState.Item(
            name = "Bobiverse 1-4",
            id = Uri.EMPTY,
            folderType = FolderType.SingleFolder,
          ),
          FolderPickerViewState.Item(
            name = "Harry Potter 1",
            id = Uri.EMPTY,
            folderType = FolderType.SingleFile,
          ),
        ),
      ),
      onAddClick = {},
      onDeleteClick = {},
      onFolderClick = {},
      onCloseClick = {},
    )
  }
}

@Composable
@Preview
private fun FolderOverviewEmptyPreview() {
  VoiceTheme {
    FolderOverviewView(
      viewState = FolderPickerViewState(items = emptyList()),
      onAddClick = {},
      onDeleteClick = {},
      onFolderClick = {},
      onCloseClick = {},
    )
  }
}
