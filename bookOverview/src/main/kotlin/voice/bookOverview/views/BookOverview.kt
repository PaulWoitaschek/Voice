package voice.bookOverview.views

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.CompareArrows
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.flowWithLifecycle
import com.squareup.anvil.annotations.ContributesTo
import voice.bookOverview.BookOverviewCategory
import voice.bookOverview.BookOverviewViewModel
import voice.bookOverview.BookOverviewViewState
import voice.bookOverview.R
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.rootComponentAs
import voice.data.Book
import java.util.UUID

@ContributesTo(AppScope::class)
interface BookOverviewComponent {
  val bookOverviewViewModel: BookOverviewViewModel
}

@Composable
fun BookOverview(
  onSettingsClick: () -> Unit,
  onBookMigrationClick: () -> Unit,
  toFolderOverview: () -> Unit,
  toEditBook: (Book.Id) -> Unit,
  toBook: (Book.Id) -> Unit,
) {
  val viewModel = remember {
    rootComponentAs<BookOverviewComponent>()
      .bookOverviewViewModel
  }
  LaunchedEffect(Unit) {
    viewModel.attach()
  }
  val lifecycleOwner = LocalLifecycleOwner.current
  val viewState by remember(lifecycleOwner, viewModel) {
    viewModel.state().flowWithLifecycle(lifecycleOwner.lifecycle, Lifecycle.State.RESUMED)
  }.collectAsState(initial = BookOverviewViewState.Loading)
  BookOverview(
    viewState = viewState,
    onLayoutIconClick = viewModel::toggleGrid,
    onSettingsClick = onSettingsClick,
    onBookClick = toBook,
    onBookFolderClick = toFolderOverview,
    onPlayButtonClick = viewModel::playPause,
    onBookMigrationClick = {
      viewModel.onBoomMigrationHelperConfirmClick()
      onBookMigrationClick()
    },
    onBoomMigrationHelperConfirmClick = viewModel::onBoomMigrationHelperConfirmClick,
    onBookLongClick = toEditBook
  )
}


@Composable
internal fun BookOverview(
  viewState: BookOverviewViewState,
  onLayoutIconClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onBookClick: (Book.Id) -> Unit,
  onBookLongClick: (Book.Id) -> Unit,
  onBookFolderClick: () -> Unit,
  onPlayButtonClick: () -> Unit,
  onBookMigrationClick: () -> Unit,
  onBoomMigrationHelperConfirmClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      SmallTopAppBar(
        title = {
          Text(text = stringResource(id = R.string.app_name))
        },
        actions = {
          if (viewState.showMigrateIcon) {
            MigrateIcon(
              onClick = onBookMigrationClick,
              withHint = viewState.showMigrateHint,
              onHintClick = onBoomMigrationHelperConfirmClick
            )
          }
          BookFolderIcon(withHint = viewState.showAddBookHint, onClick = onBookFolderClick)

          val layoutIcon = viewState.layoutIcon
          if (layoutIcon != null) {
            LayoutIcon(layoutIcon, onLayoutIconClick)
          }
          SettingsIcon(onSettingsClick)
        }
      )
    },
    floatingActionButton = {
      if (viewState.playButtonState != null) {
        PlayButton(
          playing = viewState.playButtonState == BookOverviewViewState.PlayButtonState.Playing,
          onClick = onPlayButtonClick
        )
      }
    }
  ) { contentPadding ->
    when (viewState) {
      is BookOverviewViewState.Content -> {
        when (viewState.layoutMode) {
          BookOverviewViewState.Content.LayoutMode.List -> {
            ListBooks(
              books = viewState.books,
              onBookClick = onBookClick,
              onBookLongClick = onBookLongClick,
              contentPadding = contentPadding,
            )
          }
          BookOverviewViewState.Content.LayoutMode.Grid -> {
            GridBooks(
              books = viewState.books,
              onBookClick = onBookClick,
              onBookLongClick = onBookLongClick,
              contentPadding = contentPadding,
            )
          }
        }
      }
      BookOverviewViewState.Loading -> {
        Box(
          Modifier
            .fillMaxSize()
            .padding(contentPadding)
        ) {
          CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
      }
    }
  }
}

@Composable
private fun MigrateHint(onClick: () -> Unit) {
  ExplanationTooltip {
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
      Text(stringResource(R.string.migration_hint_title), style = MaterialTheme.typography.headlineSmall)
      Spacer(modifier = Modifier.size(8.dp))
      Text(stringResource(R.string.migration_hint_content))
      Spacer(modifier = Modifier.size(16.dp))
      Button(
        modifier = Modifier.align(Alignment.End),
        onClick = onClick,
      ) {
        Text(stringResource(R.string.migration_hint_confirm))
      }
    }
  }
}


@Composable
private fun AddBookHint() {
  ExplanationTooltip {
    Text(
      modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
      text = stringResource(R.string.voice_intro_first_book)
    )
  }
}

@Composable
private fun PlayButton(playing: Boolean, onClick: () -> Unit) {
  FloatingActionButton(onClick = onClick) {
    Icon(
      painter = rememberAnimatedVectorPainter(
        animatedImageVector = AnimatedImageVector.animatedVectorResource(
          id = R.drawable.avd_pause_to_play
        ),
        atEnd = !playing
      ),
      contentDescription = stringResource(R.string.play_pause)
    )
  }
}


@Composable
private fun BookFolderIcon(modifier: Modifier = Modifier, withHint: Boolean, onClick: () -> Unit) {
  Box {
    IconButton(modifier = modifier, onClick = onClick) {
      Icon(
        imageVector = Icons.Outlined.Book,
        contentDescription = stringResource(R.string.audiobook_folders_title)
      )
    }
    if (withHint) {
      AddBookHint()
    }
  }
}

@Composable
private fun MigrateIcon(
  modifier: Modifier = Modifier,
  withHint: Boolean,
  onClick: () -> Unit,
  onHintClick: () -> Unit
) {
  Box {
    IconButton(modifier = modifier, onClick = onClick) {
      Icon(
        imageVector = Icons.Outlined.CompareArrows,
        contentDescription = stringResource(R.string.migration_hint_title)
      )
    }
    if (withHint) {
      MigrateHint(onHintClick)
    }
  }
}

@Composable
private fun SettingsIcon(onSettingsClick: () -> Unit) {
  IconButton(onSettingsClick) {
    Icon(
      imageVector = Icons.Outlined.Settings,
      contentDescription = stringResource(R.string.action_settings)
    )
  }
}

@Composable
private fun LayoutIcon(layoutIcon: BookOverviewViewState.Content.LayoutIcon, onClick: () -> Unit) {
  IconButton(onClick) {
    Icon(
      imageVector = when (layoutIcon) {
        BookOverviewViewState.Content.LayoutIcon.List -> Icons.Outlined.ViewList
        BookOverviewViewState.Content.LayoutIcon.Grid -> Icons.Outlined.GridView
      },
      contentDescription = stringResource(
        when (layoutIcon) {
          BookOverviewViewState.Content.LayoutIcon.List -> R.string.layout_list
          BookOverviewViewState.Content.LayoutIcon.Grid -> R.string.layout_grid
        }
      )
    )
  }
}

@Preview
@Composable
private fun BookOverviewPreview(
  @PreviewParameter(BookOverviewPreviewParameterProvider::class)
  viewState: BookOverviewViewState
) {
  VoiceTheme {
    BookOverview(
      viewState = viewState,
      onLayoutIconClick = {},
      onSettingsClick = {},
      onBookClick = {},
      onBookFolderClick = {},
      onPlayButtonClick = {},
      onBookLongClick = {},
      onBoomMigrationHelperConfirmClick = {},
      onBookMigrationClick = {},
    )
  }
}

internal class BookOverviewPreviewParameterProvider : PreviewParameterProvider<BookOverviewViewState> {

  fun book(): BookOverviewViewState.Content.BookViewState {
    return BookOverviewViewState.Content.BookViewState(
      name = "Book",
      author = "Author",
      cover = null,
      progress = 0.8F,
      id = Book.Id(UUID.randomUUID().toString()),
      remainingTime = "01:04"
    )
  }

  override val values = sequenceOf(
    BookOverviewViewState.Loading,
    BookOverviewViewState.Content(
      layoutIcon = BookOverviewViewState.Content.LayoutIcon.List,
      books = mapOf(
        BookOverviewCategory.CURRENT to buildList { repeat(10) { add(book()) } },
        BookOverviewCategory.FINISHED to listOf(book(), book()),
      ),
      layoutMode = BookOverviewViewState.Content.LayoutMode.List,
      playButtonState = BookOverviewViewState.PlayButtonState.Paused,
      showAddBookHint = false,
      showMigrateHint = false,
      showMigrateIcon = true
    )
  )
}
