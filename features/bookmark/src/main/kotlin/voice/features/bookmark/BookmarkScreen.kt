@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookmark

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ButtonGroupDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Text
import androidx.compose.material3.ToggleButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.strings.R
import voice.core.ui.AuroraBackground
import voice.core.ui.CoverTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberAnimationClock
import voice.features.bookmark.history.HistoryList
import voice.features.bookmark.history.HistoryViewEffect
import voice.features.bookmark.history.HistoryViewModel
import voice.features.bookmark.history.HistoryViewState
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import java.text.NumberFormat

@ContributesTo(AppScope::class)
interface Graph {
  val bookmarkViewModelFactory: BookmarkViewModel.Factory
  val historyViewModelFactory: HistoryViewModel.Factory
}

@BindingContainer
@ContributesTo(AppScope::class)
object BookmarkProvider {

  @Provides
  @IntoSet
  fun bookmarkNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.Bookmarks> { key ->
    NavEntry(key) {
      BookmarkScreen(bookId = key.bookId, editBookmarkId = key.editBookmarkId)
    }
  }
}

internal enum class BookmarkTab {
  Bookmarks,
  History,
}

@Composable
fun BookmarkScreen(
  bookId: BookId,
  editBookmarkId: String?,
) {
  val viewModel = retain(bookId.value, editBookmarkId) {
    rootGraphAs<Graph>().bookmarkViewModelFactory.create(bookId, editBookmarkId)
  }
  val historyViewModel = retain(bookId.value) {
    rootGraphAs<Graph>().historyViewModelFactory.create(bookId)
  }
  val viewState = viewModel.viewState()
  val historyViewState = historyViewModel.viewState()
  val snackbarHostState = remember { SnackbarHostState() }

  val deletedMessage = stringResource(R.string.bookmark_deleted)
  val undoLabel = stringResource(R.string.common_action_undo)
  LaunchedEffect(viewModel) {
    viewModel.viewEffects.collect { effect ->
      when (effect) {
        is BookmarkViewEffect.Deleted -> {
          val result = snackbarHostState.showSnackbar(
            message = deletedMessage,
            actionLabel = undoLabel,
            duration = SnackbarDuration.Short,
          )
          if (result == SnackbarResult.ActionPerformed) {
            viewModel.onUndoDelete(effect.bookmark)
          }
        }
      }
    }
  }
  val pinnedMessage = stringResource(R.string.history_pinned)
  val restoredMessage = stringResource(R.string.history_restored)
  val changedBackMessage = stringResource(R.string.history_changed_back)
  LaunchedEffect(historyViewModel) {
    historyViewModel.viewEffects.collect { effect ->
      snackbarHostState.showSnackbar(
        when (effect) {
          HistoryViewEffect.Pinned -> pinnedMessage
          HistoryViewEffect.Restored -> restoredMessage
          HistoryViewEffect.ChangedBack -> changedBackMessage
        },
      )
    }
  }

  if (viewState == null) return
  CoverTheme(cover = viewState.cover) {
    BookmarkScreen(
      viewState = viewState,
      historyViewState = historyViewState,
      snackbarHostState = snackbarHostState,
      onClose = viewModel::onCloseClick,
      onSortChange = viewModel::onSortChange,
      onSaveClick = viewModel::onSaveClick,
      bookmarkList = { listState, contentPadding ->
        BookmarkList(
          viewState = viewState,
          listState = listState,
          contentPadding = contentPadding,
          onCategoryClick = viewModel::onCategoryClick,
          onClick = viewModel::onBookmarkClick,
          onLongClick = viewModel::onBookmarkLongClick,
          onDelete = viewModel::onDelete,
        )
      },
      historyList = { contentPadding ->
        if (historyViewState != null) {
          HistoryList(
            viewState = historyViewState,
            contentPadding = contentPadding,
            onFilterClick = historyViewModel::onFilterClick,
            onSourceChange = historyViewModel::onSourceChange,
            onActionClick = historyViewModel::onActionClick,
            onSuggestionBack = historyViewModel::onSuggestionBack,
            onSuggestionKeep = historyViewModel::onSuggestionKeep,
          )
        }
      },
    )
    val editor = viewState.editor
    if (editor != null) {
      BookmarkEditorSheet(
        editor = editor,
        onNoteChange = viewModel::onNoteChange,
        onKindChange = viewModel::onKindChange,
        onSleepKindClick = viewModel::onSleepKindClick,
        onMoveEarlier = viewModel::onMoveEarlier,
        onMoveLater = viewModel::onMoveLater,
        onUndo = viewModel::onEditorUndo,
        onDelete = viewModel::onEditorDelete,
        onDone = viewModel::onEditorDone,
      )
    }
  }
}

@Composable
private fun BookmarkScreen(
  viewState: BookmarkViewState,
  historyViewState: HistoryViewState?,
  snackbarHostState: SnackbarHostState,
  onClose: () -> Unit,
  onSortChange: (BookmarkSort) -> Unit,
  onSaveClick: () -> Unit,
  bookmarkList: @Composable (LazyListState, PaddingValues) -> Unit,
  historyList: @Composable (PaddingValues) -> Unit,
) {
  var tab by rememberSaveable { mutableStateOf(BookmarkTab.Bookmarks) }
  val listState = rememberLazyListState()
  OpenWhereYouAre(viewState, listState)
  val clock = rememberAnimationClock(running = viewState.playing)
  Box(Modifier.fillMaxSize()) {
    AuroraBackground(
      clock = { clock.value },
      showStars = viewState.sleepTimerActive,
      modifier = Modifier.fillMaxSize(),
    )
    Scaffold(
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      snackbarHost = { SnackbarHost(snackbarHostState) },
      topBar = {
        BookmarkTopBar(
          title = viewState.bookTitle,
          author = viewState.bookAuthor,
          sort = viewState.sort.takeIf { tab == BookmarkTab.Bookmarks },
          onSortChange = onSortChange,
          onClose = onClose,
        )
      },
      floatingActionButton = {
        if (tab == BookmarkTab.Bookmarks) {
          ExtendedFloatingActionButton(
            text = { Text(stringResource(R.string.bookmark_save_moment)) },
            icon = { Icon(VoiceIcons.BookmarkAdd, contentDescription = null) },
            onClick = onSaveClick,
            expanded = !listState.canScrollBackward || !listState.isScrollInProgress,
          )
        }
      },
    ) { contentPadding ->
      // the list scrolls behind the navigation bar at the bottom, but not behind one at the side
      val layoutDirection = LocalLayoutDirection.current
      Column(
        Modifier.padding(
          start = contentPadding.calculateStartPadding(layoutDirection),
          top = contentPadding.calculateTopPadding(),
          end = contentPadding.calculateEndPadding(layoutDirection),
        ),
      ) {
        Tabs(
          tab = tab,
          bookmarkCount = viewState.totalCount,
          onTabChange = { tab = it },
        )
        val listPadding = PaddingValues(bottom = contentPadding.calculateBottomPadding())
        when (tab) {
          BookmarkTab.Bookmarks -> bookmarkList(listState, listPadding)
          BookmarkTab.History -> historyList(listPadding)
        }
      }
    }
  }
}

/** Starts the list at "You are here", with a bit of what's behind it still showing. */
@Composable
private fun OpenWhereYouAre(
  viewState: BookmarkViewState,
  listState: LazyListState,
) {
  var scrolled by rememberSaveable { mutableStateOf(false) }
  val index = viewState.items.indexOfFirst { it.key == YOU_ARE_HERE_KEY }
  LaunchedEffect(index) {
    if (!scrolled && index != -1 && viewState.sort == BookmarkSort.Story) {
      scrolled = true
      if (index > 2) {
        listState.scrollToItem(index + HEADER_ITEMS - 2)
      }
    }
  }
}

@Composable
private fun BookmarkTopBar(
  title: String,
  author: String?,
  sort: BookmarkSort?,
  onSortChange: (BookmarkSort) -> Unit,
  onClose: () -> Unit,
) {
  TopAppBar(
    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
    navigationIcon = {
      IconButton(onClick = onClose) {
        Icon(VoiceIcons.ArrowBack, contentDescription = stringResource(R.string.common_action_back))
      }
    },
    title = {
      Column {
        Text(
          text = title,
          style = MaterialTheme.typography.titleMediumEmphasized,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        if (author != null) {
          Text(
            text = author,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
          )
        }
      }
    },
    actions = {
      if (sort != null) {
        SortMenu(sort = sort, onSortChange = onSortChange)
      }
    },
  )
}

@Composable
private fun SortMenu(
  sort: BookmarkSort,
  onSortChange: (BookmarkSort) -> Unit,
) {
  var expanded by remember { mutableStateOf(false) }
  Box {
    IconButton(onClick = { expanded = true }) {
      Icon(VoiceIcons.Sort, contentDescription = stringResource(R.string.bookmark_sort))
    }
    DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
      BookmarkSort.entries.forEach { option ->
        DropdownMenuItem(
          text = {
            Text(
              stringResource(
                when (option) {
                  BookmarkSort.Story -> R.string.bookmark_sort_story
                  BookmarkSort.Recent -> R.string.bookmark_sort_recent
                },
              ),
            )
          },
          trailingIcon = if (option == sort) {
            { Icon(VoiceIcons.Check, contentDescription = null) }
          } else {
            null
          },
          onClick = {
            expanded = false
            onSortChange(option)
          },
        )
      }
    }
  }
}

@Composable
private fun Tabs(
  tab: BookmarkTab,
  bookmarkCount: Int,
  onTabChange: (BookmarkTab) -> Unit,
) {
  val locale = LocalConfiguration.current.locales[0]
  val countFormat = remember(locale) { NumberFormat.getIntegerInstance(locale) }
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp, vertical = 8.dp),
    horizontalArrangement = Arrangement.spacedBy(ButtonGroupDefaults.ConnectedSpaceBetween),
  ) {
    TabButton(
      checked = tab == BookmarkTab.Bookmarks,
      onClick = { onTabChange(BookmarkTab.Bookmarks) },
      icon = VoiceIcons.CollectionsBookmark,
      label = stringResource(R.string.bookmark_tab_bookmarks),
      badge = bookmarkCount.takeIf { it > 0 }?.let { countFormat.format(it.toLong()) },
      leading = true,
      modifier = Modifier.weight(1F),
    )
    TabButton(
      checked = tab == BookmarkTab.History,
      onClick = { onTabChange(BookmarkTab.History) },
      icon = VoiceIcons.History,
      label = stringResource(R.string.bookmark_tab_history),
      badge = null,
      leading = false,
      modifier = Modifier.weight(1F),
    )
  }
}

@Composable
private fun TabButton(
  checked: Boolean,
  onClick: () -> Unit,
  icon: ImageVector,
  label: String,
  badge: String?,
  leading: Boolean,
  modifier: Modifier = Modifier,
) {
  ToggleButton(
    checked = checked,
    onCheckedChange = { onClick() },
    modifier = modifier,
    shapes = if (leading) {
      ButtonGroupDefaults.connectedLeadingButtonShapes()
    } else {
      ButtonGroupDefaults.connectedTrailingButtonShapes()
    },
  ) {
    Icon(icon, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
    Spacer(Modifier.width(ButtonDefaults.IconSpacing))
    Text(
      text = label,
      maxLines = 1,
      overflow = TextOverflow.Ellipsis,
    )
    if (badge != null) {
      Spacer(Modifier.width(6.dp))
      Text(
        text = badge,
        style = MaterialTheme.typography.labelLarge.copy(fontFeatureSettings = "tnum"),
      )
    }
  }
}
