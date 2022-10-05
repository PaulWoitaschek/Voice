package voice.bookOverview.search

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
import androidx.compose.material.icons.outlined.History
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalMinimumTouchTargetEnforcement
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.google.accompanist.flowlayout.FlowMainAxisAlignment
import com.google.accompanist.flowlayout.FlowRow
import voice.bookOverview.R
import voice.bookOverview.overview.BookOverviewLayoutMode
import voice.bookOverview.views.GridBook
import voice.bookOverview.views.ListBookRow
import voice.bookOverview.views.gridColumnCount
import voice.common.compose.plus
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs

@Composable
fun BookSearchScreen(modifier: Modifier = Modifier) {
  val viewModel = rememberScoped {
    rootComponentAs<BookSearchComponent>().bookSearchViewModel
  }
  val viewState = viewModel.viewState()
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    focusRequester.requestFocus()
  }
  val scrollBehavior = TopAppBarDefaults.pinnedScrollBehavior()
  Scaffold(
    modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
    topBar = {
      TopAppBar(
        title = {},
        scrollBehavior = scrollBehavior,
        navigationIcon = {
          IconButton(onClick = viewModel::onCloseClick) {
            Icon(
              imageVector = Icons.Outlined.ArrowBack,
              contentDescription = stringResource(id = R.string.close),
            )
          }
        },
        actions = {
          TextField(
            modifier = Modifier
              .focusRequester(focusRequester)
              .padding(start = 54.dp, end = 8.dp)
              .fillMaxWidth(),
            colors = TextFieldDefaults.textFieldColors(
              containerColor = Color.Transparent,
              focusedIndicatorColor = Color.Transparent,
              unfocusedIndicatorColor = Color.Transparent,
            ),
            value = viewState.query,
            singleLine = true,
            maxLines = 1,
            onValueChange = viewModel::onNewSearch,
            label = {
              Text(stringResource(id = R.string.search_hint))
            },
          )
        },
      )
    },
    content = { contentPadding ->
      when (viewState) {
        is BookSearchViewState.InactiveSearch -> {
          FlowRow(
            modifier = Modifier
              .verticalScroll(rememberScrollState())
              .fillMaxSize()
              .padding(contentPadding)
              .padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 24.dp),
            mainAxisSpacing = 8.dp,
            crossAxisSpacing = 8.dp,
            mainAxisAlignment = FlowMainAxisAlignment.Center,
            content = {
              CompositionLocalProvider(LocalMinimumTouchTargetEnforcement provides false) {
                viewState.recentQueries.forEach { query ->
                  SuggestionChip(
                    onClick = { viewModel.onNewSearch(query) },
                    label = {
                      Text(text = query)
                    },
                    icon = {
                      Icon(
                        imageVector = Icons.Outlined.History,
                        contentDescription = null,
                      )
                    },
                  )
                }
                viewState.suggestedAuthors.forEach { author ->
                  SuggestionChip(
                    onClick = { viewModel.onNewSearch(author) },
                    label = {
                      Text(text = author)
                    },
                  )
                }
              }
            },
          )
        }
        is BookSearchViewState.SearchResults -> {
          when (viewState.layoutMode) {
            BookOverviewLayoutMode.List -> {
              LazyColumn(
                contentPadding = PaddingValues(vertical = 16.dp),
                modifier = Modifier
                  .padding(contentPadding)
                  .padding(horizontal = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                content = {
                  items(viewState.books) { book ->
                    ListBookRow(
                      book = book,
                      onBookClick = viewModel::onBookClick,
                      onBookLongClick = viewModel::onBookClick,
                    )
                  }
                },
              )
            }
            BookOverviewLayoutMode.Grid -> {
              LazyVerticalGrid(
                columns = GridCells.Fixed(gridColumnCount()),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = contentPadding + PaddingValues(start = 8.dp, end = 8.dp, top = 24.dp, bottom = 4.dp),
                content = {
                  items(viewState.books) { book ->
                    GridBook(
                      book = book,
                      onBookClick = viewModel::onBookClick,
                      onBookLongClick = viewModel::onBookClick,
                    )
                  }
                },
              )
            }
          }
        }
      }
    },
  )
}
