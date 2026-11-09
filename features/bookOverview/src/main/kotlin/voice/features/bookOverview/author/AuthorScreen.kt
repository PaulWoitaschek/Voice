@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.author

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsBottomHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import coil.compose.AsyncImage
import coil.request.ImageRequest
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.strings.R as StringsR
import voice.core.ui.CenteredContent
import voice.core.ui.R as UiR
import voice.core.ui.VoiceTheme
import voice.core.ui.bookText
import voice.core.ui.segmentedShape
import voice.features.bookOverview.overview.SeriesStackViewState
import voice.features.bookOverview.views.BookOverviewPreviewParameterProvider
import voice.features.bookOverview.views.ListBookRow
import voice.features.bookOverview.views.ListSeriesRow
import voice.features.bookOverview.views.seriesCounts
import voice.navigation.Destination
import voice.navigation.NavEntryProvider

@BindingContainer
@ContributesTo(AppScope::class)
object AuthorProvider {

  @Provides
  @IntoSet
  fun authorNavEntryProvider(): NavEntryProvider<*> =
    NavEntryProvider<Destination.Author> { key ->
      NavEntry(key) {
        AuthorScreen(name = key.name)
      }
    }
}

@Composable
fun AuthorScreen(name: String) {
  val viewModel =
    retain(name) {
      rootGraphAs<AuthorViewModel.Factory.Provider>().authorViewModelFactory.create(name)
    }
  val viewState = viewModel.viewState() ?: return
  AuthorScreen(
    viewState = viewState,
    onCloseClick = viewModel::onCloseClick,
    onBookClick = viewModel::onBookClick,
    onBookLongClick = viewModel::onBookLongClick,
    onSeriesClick = viewModel::onSeriesClick,
    onSeriesLongClick = viewModel::onSeriesLongClick,
    onLinkClick = viewModel::onLinkClick,
  )
}

@Composable
private fun AuthorScreen(
  viewState: AuthorViewState,
  onCloseClick: () -> Unit,
  onBookClick: (BookId) -> Unit,
  onBookLongClick: (BookId) -> Unit,
  onSeriesClick: (String) -> Unit,
  onSeriesLongClick: (String) -> Unit,
  onLinkClick: (String) -> Unit,
) {
  Scaffold(
    topBar = {
      TopAppBar(
        title = {},
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
        contentPadding =
          PaddingValues(
            start = horizontalPadding,
            end = horizontalPadding,
            top = 8.dp,
            bottom = 24.dp,
          ),
      ) {
        item(key = "header") {
          Header(viewState)
        }
        val info = viewState.info
        if (info != null) {
          item(key = "about") {
            About(
              info = info,
              onLinkClick = onLinkClick,
              modifier = Modifier.animateItem().padding(top = 24.dp),
            )
          }
        }
        item(key = "booksSpacer") {
          Spacer(Modifier.animateItem().height(24.dp))
        }
        itemsIndexed(viewState.entries, key = { _, entry -> entry.key }) { index, entry ->
          val shape = segmentedShape(index, viewState.entries.size)
          val modifier = Modifier.animateItem().padding(bottom = 2.dp)
          when (entry) {
            is AuthorViewState.Entry.Book ->
              ListBookRow(
                book = entry.book,
                onBookClick = onBookClick,
                onBookLongClick = onBookLongClick,
                shape = shape,
                finished = entry.finished,
                modifier = modifier,
              )
            is AuthorViewState.Entry.Series ->
              ListSeriesRow(
                series = entry.series,
                onClick = { onSeriesClick(entry.series.name) },
                onLongClick = { onSeriesLongClick(entry.series.name) },
                shape = shape,
                modifier = modifier,
              )
          }
        }
        item(key = "navigationBarSpacer") {
          Spacer(Modifier.windowInsetsBottomHeight(WindowInsets.navigationBars))
        }
      }
    }
  }
}

@Composable
private fun Header(viewState: AuthorViewState) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Portrait(photo = viewState.info?.photo?.url, modifier = Modifier.size(112.dp))
    Spacer(Modifier.width(20.dp))
    Column(Modifier.weight(1F)) {
      Text(
        text = viewState.name,
        style = MaterialTheme.typography.headlineMediumEmphasized.bookText(),
        maxLines = 3,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.semantics { heading() },
      )
      val description = viewState.info?.description
      if (description != null) {
        Spacer(Modifier.height(4.dp))
        Text(
          text = description,
          style = MaterialTheme.typography.bodyLarge,
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
private fun Portrait(
  photo: String?,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  Box(
    modifier =
      modifier.clip(MaterialShapes.Cookie12Sided.toShape()).background(colors.secondaryContainer),
    contentAlignment = Alignment.Center,
  ) {
    Icon(
      painter = painterResource(UiR.drawable.ic_person),
      contentDescription = null,
      tint = colors.onSecondaryContainer,
      modifier = Modifier.size(56.dp),
    )
    if (photo != null) {
      val context = LocalContext.current
      AsyncImage(
        model =
          remember(photo, context) {
            ImageRequest.Builder(context).data(photo).crossfade(true).build()
          },
        contentDescription = null,
        contentScale = ContentScale.Crop,
        // faces sit in the upper half of a portrait
        alignment = BiasAlignment(horizontalBias = 0F, verticalBias = -0.5F),
        modifier = Modifier.fillMaxSize(),
      )
    }
  }
}

@Composable
private fun About(
  info: AuthorInfo,
  onLinkClick: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  var expanded by rememberSaveable { mutableStateOf(false) }
  Surface(
    onClick = { expanded = !expanded },
    modifier = modifier.fillMaxWidth(),
    shape = RoundedCornerShape(32.dp),
    color = MaterialTheme.colorScheme.surfaceContainer,
  ) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 20.dp, bottom = 16.dp)) {
      Text(
        text = info.extract,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = if (expanded) Int.MAX_VALUE else 4,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.animateContentSize(MaterialTheme.motionScheme.defaultSpatialSpec()),
      )
      Spacer(Modifier.height(4.dp))
      // lines the label up with the text
      TextButton(onClick = { onLinkClick(info.url) }, modifier = Modifier.offset(x = (-12).dp)) {
        Text(stringResource(StringsR.string.author_wikipedia))
      }
      Credits(photo = info.photo, onLinkClick = onLinkClick)
    }
  }
}

// The text and the photo may only be shown with their license. The Commons page of the photo
// names its own license and the photographer.
@Composable
private fun Credits(
  photo: AuthorPhoto?,
  onLinkClick: (String) -> Unit,
) {
  val linkStyles = TextLinkStyles(SpanStyle(textDecoration = TextDecoration.Underline))
  val text = buildAnnotatedString {
    withLink(LinkAnnotation.Url(TEXT_LICENSE, linkStyles) { onLinkClick(TEXT_LICENSE) }) {
      append("CC BY-SA 4.0")
    }
    if (photo != null) {
      append(" · ")
      withLink(LinkAnnotation.Url(photo.page, linkStyles) { onLinkClick(photo.page) }) {
        append("Wikimedia Commons")
      }
    }
  }
  Text(
    text = text,
    style = MaterialTheme.typography.labelMedium,
    color = MaterialTheme.colorScheme.onSurfaceVariant,
  )
}

private const val TEXT_LICENSE = "https://creativecommons.org/licenses/by-sa/4.0/"

@Composable
@Preview
private fun AuthorScreenPreview() {
  val book = BookOverviewPreviewParameterProvider().book()
  VoiceTheme {
    AuthorScreen(
      viewState =
        AuthorViewState(
          name = "Daniel Hartwell",
          bookCount = 6,
          finishedCount = 2,
          info =
            AuthorInfo(
              description = "American science fiction writer (born 1971)",
              extract =
                "Daniel Hartwell is an American author of science fiction. He is best known " +
                  "for The Signal Saga, a series about a radio astronomer who hears a message " +
                  "from her own future. His novels have been translated into more than twenty " +
                  "languages.",
              photo = null,
              url = "https://en.wikipedia.org",
            ),
          entries =
            listOf(
              AuthorViewState.Entry.Book(
                book.copy(id = BookId("1"), name = "The Quiet Orbit", author = null),
                finished = false,
              ),
              AuthorViewState.Entry.Series(
                SeriesStackViewState(
                  key = "thesignalsaga",
                  name = "The Signal Saga",
                  author = null,
                  covers = listOf(null, null, null),
                  bookCount = 4,
                  finishedCount = 1,
                  progress = listOf(1F, 0.4F, 0F, 0F),
                  category = BookCategory.CURRENT,
                ),
              ),
              AuthorViewState.Entry.Book(
                book.copy(id = BookId("2"), name = "Low Tide", author = null, progress = 1F),
                finished = true,
              ),
            ),
        ),
      onCloseClick = {},
      onBookClick = {},
      onBookLongClick = {},
      onSeriesClick = {},
      onSeriesLongClick = {},
      onLinkClick = {},
    )
  }
}
