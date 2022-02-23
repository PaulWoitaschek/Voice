package voice.bookOverview.views

import androidx.compose.animation.graphics.res.animatedVectorResource
import androidx.compose.animation.graphics.res.rememberAnimatedVectorPainter
import androidx.compose.animation.graphics.vector.AnimatedImageVector
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.GenericShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Book
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallTopAppBar
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathOperation
import androidx.compose.ui.graphics.addOutline
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import voice.bookOverview.BookOverviewCategory
import voice.bookOverview.BookOverviewViewState
import voice.bookOverview.R
import voice.common.compose.VoiceTheme
import voice.data.Book
import java.util.UUID

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun BookOverview(
  viewState: BookOverviewViewState,
  onLayoutIconClick: () -> Unit,
  onSettingsClick: () -> Unit,
  onBookClick: (Book.Id) -> Unit,
  onBookFolderClick: () -> Unit,
  onPlayButtonClick: () -> Unit,
) {
  Scaffold(
    topBar = {
      SmallTopAppBar(
        title = {
          Text(text = stringResource(id = R.string.app_name))
        },
        actions = {
          Box {
            var bookIconCenter: Float? by remember { mutableStateOf(null) }
            BookFolderIcon(
              modifier = Modifier.onGloballyPositioned {
                bookIconCenter = it.positionInWindow().x + it.size.width / 2F
              },
              onClick = onBookFolderClick
            )
            if (viewState.showAddBookHint) {
              AddBookHint(bookIconCenter)
            }
          }
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
  ) {
    when (viewState) {
      is BookOverviewViewState.Content -> {
        when (viewState.layoutMode) {
          BookOverviewViewState.Content.LayoutMode.List -> {
            ListBooks(viewState, onBookClick)
          }
          BookOverviewViewState.Content.LayoutMode.Grid -> {
            GridBooks(viewState, onBookClick)
          }
        }
      }
      BookOverviewViewState.Loading -> {
        Box(Modifier.fillMaxSize()) {
          CircularProgressIndicator(Modifier.align(Alignment.Center))
        }
      }
    }
  }
}

@Composable
private fun AddBookHint(bookIconCenterX: Float?) {
  val density = LocalDensity.current
  val rightMargin = with(density) {
    16.dp.toPx()
  }
  var flagC by remember { mutableStateOf(0F) }
  Popup(popupPositionProvider = object : PopupPositionProvider {
    override fun calculatePosition(
      anchorBounds: IntRect,
      windowSize: IntSize,
      layoutDirection: LayoutDirection,
      popupContentSize: IntSize
    ): IntOffset {
      var offset = IntOffset(anchorBounds.center.x - popupContentSize.width / 2, anchorBounds.bottom)
      if ((offset.x + popupContentSize.width + rightMargin) > windowSize.width) {
        offset -= IntOffset(rightMargin.toInt() + (offset.x + popupContentSize.width - windowSize.width), 0)
      }
      if (bookIconCenterX != null) {
        flagC = bookIconCenterX - offset.x
      }
      return offset
    }
  }) {
    val triangleSize = with(density) {
      28.dp.toPx()
    }
    Card(
      modifier = Modifier.widthIn(max = 240.dp),
      elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
      shape = GenericShape { size, layoutDirection ->
        addOutline(RoundedCornerShape(12.0.dp).createOutline(size, layoutDirection, density))
        val trianglePath = Path().apply {
          moveTo(
            x = flagC - triangleSize / 2F,
            y = 0F
          )
          lineTo(
            x = flagC,
            y = -triangleSize / 2F
          )
          lineTo(
            x = flagC + triangleSize / 2F,
            y = 0F
          )
          close()
        }
        op(this, trianglePath, PathOperation.Union)
      }
    ) {
      Text(
        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
        text = stringResource(R.string.voice_intro_first_book)
      )
    }
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
      contentDescription = "todo"
    )
  }
}


@Composable
private fun BookFolderIcon(modifier: Modifier = Modifier, onClick: () -> Unit) {
  IconButton(modifier = modifier, onClick = onClick) {
    Icon(
      imageVector = Icons.Outlined.Book,
      contentDescription = stringResource(R.string.audiobook_folders_title)
    )
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
      onPlayButtonClick = {}
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
      showAddBookHint = true,
    )
  )
}
