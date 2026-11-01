@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.series

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.strings.R as StringsR
import voice.core.ui.CenteredContent
import voice.core.ui.CoverTheme
import voice.core.ui.VoiceTheme
import voice.core.ui.bookText
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.segmentedShape
import voice.features.bookOverview.views.BookOverviewPreviewParameterProvider
import voice.features.bookOverview.views.ListBookRow
import voice.features.bookOverview.views.SeriesCover
import voice.features.bookOverview.views.seriesCounts
import voice.navigation.Destination
import voice.navigation.NavEntryProvider

@BindingContainer
@ContributesTo(AppScope::class)
object SeriesProvider {

  @Provides
  @IntoSet
  fun seriesNavEntryProvider(): NavEntryProvider<*> =
    NavEntryProvider<Destination.Series> { key ->
      NavEntry(key) {
        SeriesScreen(name = key.name)
      }
    }
}

@Composable
fun SeriesScreen(name: String) {
  val viewModel =
    retain(name) {
      rootGraphAs<SeriesViewModel.Factory.Provider>().seriesViewModelFactory.create(name)
    }
  val viewState = viewModel.viewState() ?: return
  CoverTheme(cover = viewState.covers.first()) {
    SeriesScreen(
      viewState = viewState,
      onCloseClick = viewModel::onCloseClick,
      onActionClick = viewModel::onActionClick,
      onBookClick = viewModel::onBookClick,
      onBookLongClick = viewModel::onBookLongClick,
    )
  }
}

@Composable
private fun SeriesScreen(
  viewState: SeriesViewState,
  onCloseClick: () -> Unit,
  onActionClick: (BookId) -> Unit,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {},
        navigationIcon = {
          IconButton(onClick = onCloseClick) {
            Icon(VoiceIcons.ArrowBack, contentDescription = stringResource(StringsR.string.common_action_back))
          }
        },
      )
    },
    contentWindowInsets = WindowInsets(0, 0, 0, 0),
  ) { contentPadding ->
    val layoutDirection = LocalLayoutDirection.current
    CenteredContent(
      modifier =
        Modifier.padding(
          start = contentPadding.calculateStartPadding(layoutDirection),
          top = contentPadding.calculateTopPadding(),
          end = contentPadding.calculateEndPadding(layoutDirection),
        ),
    ) { horizontalPadding ->
      LazyColumn(
        contentPadding = PaddingValues(start = horizontalPadding, end = horizontalPadding, top = 8.dp, bottom = 24.dp),
      ) {
        item(key = "header") {
          Header(viewState)
        }
        val action = viewState.action
        if (action != null) {
          item(key = "action") {
            ActionButton(
              action = action,
              onClick = { onActionClick(action.bookId) },
              modifier = Modifier.padding(top = 24.dp),
            )
          }
        }
        item(key = "booksSpacer") {
          Spacer(Modifier.height(24.dp))
        }
        itemsIndexed(viewState.books, key = { _, book -> book.book.id.value }) { index, book ->
          ListBookRow(
            book = book.book,
            onBookClick = onBookClick,
            onBookLongClick = onBookLongClick,
            shape = segmentedShape(index, viewState.books.size),
            finished = book.finished,
            part = book.part,
            current = book.current,
            playing = book.playing,
            modifier = Modifier.padding(bottom = 2.dp),
          )
        }
        item(key = "navigationBarSpacer") {
          Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
      }
    }
  }
}

@Composable
private fun Header(viewState: SeriesViewState) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    SeriesCover(
      seriesKey = viewState.key,
      covers = viewState.covers,
      modifier = Modifier.size(136.dp),
    )
    Spacer(Modifier.width(20.dp))
    Column(Modifier.weight(1F)) {
      Text(
        text = viewState.name,
        style = MaterialTheme.typography.headlineMediumEmphasized.bookText(),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() },
      )
      if (viewState.author != null) {
        Spacer(Modifier.height(4.dp))
        Text(
          text = viewState.author,
          style = MaterialTheme.typography.bodyLarge.bookText(),
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          maxLines = 2,
          overflow = TextOverflow.Ellipsis,
        )
      }
      Spacer(Modifier.height(4.dp))
      Text(
        text = seriesCounts(viewState.bookCount, viewState.finishedCount),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

@Composable
private fun ActionButton(
  action: SeriesViewState.Action,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val haptics = LocalHapticFeedback.current
  val label =
    when {
      action.playing -> stringResource(StringsR.string.playback_action_pause)
      action.continues && action.part != null -> stringResource(StringsR.string.series_continue_book, action.part)
      action.continues -> stringResource(StringsR.string.series_continue)
      action.part != null -> stringResource(StringsR.string.series_start_book, action.part)
      else -> stringResource(StringsR.string.series_start)
    }
  Button(
    onClick = {
      haptics.performHapticFeedback(if (action.playing) HapticFeedbackType.ToggleOff else HapticFeedbackType.ToggleOn)
      onClick()
    },
    modifier = modifier.fillMaxWidth().heightIn(min = ButtonDefaults.MediumContainerHeight),
    contentPadding = ButtonDefaults.contentPaddingFor(ButtonDefaults.MediumContainerHeight),
  ) {
    Icon(
      imageVector = if (action.playing) VoiceIcons.Pause else VoiceIcons.PlayArrow,
      contentDescription = null,
      modifier = Modifier.size(ButtonDefaults.iconSizeFor(ButtonDefaults.MediumContainerHeight)),
    )
    Spacer(Modifier.width(ButtonDefaults.iconSpacingFor(ButtonDefaults.MediumContainerHeight)))
    Text(label, style = ButtonDefaults.textStyleFor(ButtonDefaults.MediumContainerHeight))
  }
}

@Composable
@Preview
private fun SeriesScreenPreview() {
  val book = BookOverviewPreviewParameterProvider().book()
  VoiceTheme {
    SeriesScreen(
      viewState =
        SeriesViewState(
          key = "thesignalsaga",
          name = "The Signal Saga",
          author = "Daniel Hartwell",
          covers = listOf(null, null, null),
          bookCount = 4,
          finishedCount = 1,
          action = SeriesViewState.Action(BookId("2"), part = "2", continues = true, playing = true),
          books =
            listOf(
              SeriesViewState.Book(
                book.copy(id = BookId("1"), name = "Static on the Line", author = null, progress = 1F),
                "1",
                finished = true,
                current = false,
                playing = false,
              ),
              SeriesViewState.Book(
                book.copy(id = BookId("2"), name = "Echoes of Tomorrow", author = null, progress = 0.72F),
                "2",
                finished = false,
                current = true,
                playing = true,
              ),
              SeriesViewState.Book(
                book.copy(id = BookId("3"), name = "Interference", author = null, progress = 0F),
                "3",
                finished = false,
                current = false,
                playing = false,
              ),
              SeriesViewState.Book(
                book.copy(id = BookId("4"), name = "Mira's Theory", author = null, progress = 0F),
                "4",
                finished = false,
                current = false,
                playing = false,
              ),
            ),
        ),
      onCloseClick = {},
      onActionClick = {},
      onBookClick = {},
      onBookLongClick = {},
    )
  }
}
