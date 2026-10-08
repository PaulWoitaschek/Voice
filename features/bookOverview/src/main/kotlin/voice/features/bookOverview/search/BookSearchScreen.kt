@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.search

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.search.BookSearchField
import voice.core.ui.icons.VoiceIcons
import voice.features.bookOverview.di.BookOverviewGraph
import voice.features.bookOverview.views.BookActions
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@BindingContainer
@ContributesTo(AppScope::class)
object BookSearchProvider {

  @Provides
  @IntoSet
  fun bookSearchNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.LibrarySearch> { key ->
    NavEntry(key) {
      BookSearchScreen()
    }
  }
}

@Composable
fun BookSearchScreen(modifier: Modifier = Modifier) {
  val bookGraph = retain<BookOverviewGraph> {
    rootGraphAs<BookOverviewGraph.Factory.Provider>()
      .bookOverviewGraphProviderFactory.create()
  }
  val viewModel = bookGraph.bookSearchViewModel
  rememberSaveable(saver = viewModel.saver) { viewModel }
  val viewState = viewModel.state()

  var showBottomSheet by remember { mutableStateOf(false) }
  BookSearch(
    viewState = viewState,
    query = viewModel.query,
    listener = object : BookSearchListener {
      override fun onBackClick() = viewModel.onBackClick()
      override fun onSearch() = viewModel.onSearch()
      override fun onRecentSearchClick(recentSearch: String) = viewModel.onRecentSearchClick(recentSearch)
      override fun onRemoveRecentSearch(recentSearch: String) = viewModel.onRemoveRecentSearch(recentSearch)
      override fun onClearRecentSearches() = viewModel.onClearRecentSearches()
      override fun onBrowseCategoryClick(category: BrowseCategory) = viewModel.onBrowseCategoryClick(category)
      override fun onBrowseEntryClick(
        category: BrowseCategory,
        name: String,
      ) = viewModel.onBrowseEntryClick(category, name)

      override fun onFilterClick(field: BookSearchField?) = viewModel.onFilterClick(field)
      override fun onBookClick(id: BookId) = viewModel.onBookClick(id)
      override fun onBookLongClick(id: BookId) {
        bookGraph.bottomSheetViewModel.bookSelected(id)
        showBottomSheet = true
      }

      override fun onPlayClick(id: BookId) = viewModel.onPlayClick(id)
      override fun onFoldersClick() = viewModel.onFoldersClick()
      override fun onSearchEverythingClick() = viewModel.onSearchEverythingClick()
    },
    modifier = modifier,
  )
  BookActions(
    bookGraph = bookGraph,
    showBottomSheet = showBottomSheet,
    onBottomSheetDismiss = { showBottomSheet = false },
  )
}

internal interface BookSearchListener {
  fun onBackClick()
  fun onSearch()
  fun onRecentSearchClick(recentSearch: String)
  fun onRemoveRecentSearch(recentSearch: String)
  fun onClearRecentSearches()
  fun onBrowseCategoryClick(category: BrowseCategory)
  fun onBrowseEntryClick(
    category: BrowseCategory,
    name: String,
  )

  fun onFilterClick(field: BookSearchField?)
  fun onBookClick(id: BookId)
  fun onBookLongClick(id: BookId)
  fun onPlayClick(id: BookId)
  fun onFoldersClick()
  fun onSearchEverythingClick()

  companion object {
    val Noop = object : BookSearchListener {
      override fun onBackClick() {}
      override fun onSearch() {}
      override fun onRecentSearchClick(recentSearch: String) {}
      override fun onRemoveRecentSearch(recentSearch: String) {}
      override fun onClearRecentSearches() {}
      override fun onBrowseCategoryClick(category: BrowseCategory) {}
      override fun onBrowseEntryClick(
        category: BrowseCategory,
        name: String,
      ) {}

      override fun onFilterClick(field: BookSearchField?) {}
      override fun onBookClick(id: BookId) {}
      override fun onBookLongClick(id: BookId) {}
      override fun onPlayClick(id: BookId) {}
      override fun onFoldersClick() {}
      override fun onSearchEverythingClick() {}
    }
  }
}

@Composable
internal fun BookSearch(
  viewState: BookSearchViewState,
  query: TextFieldState,
  listener: BookSearchListener,
  modifier: Modifier = Modifier,
) {
  val keyboard = LocalSoftwareKeyboardController.current
  // scrolling the results means reading them, so the keyboard makes room
  val hideKeyboardOnScroll = remember(keyboard) {
    object : NestedScrollConnection {
      override fun onPreScroll(
        available: Offset,
        source: NestedScrollSource,
      ): Offset {
        if (source == NestedScrollSource.UserInput && available.y != 0F) {
          keyboard?.hide()
        }
        return Offset.Zero
      }
    }
  }
  Column(
    modifier = modifier
      .fillMaxSize()
      .background(MaterialTheme.colorScheme.surface)
      .statusBarsPadding(),
  ) {
    SearchField(
      query = query,
      onSearch = {
        keyboard?.hide()
        listener.onSearch()
      },
      onBackClick = listener::onBackClick,
      modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 16.dp),
    )
    Surface(
      modifier = Modifier
        .fillMaxSize()
        .nestedScroll(hideKeyboardOnScroll),
      color = MaterialTheme.colorScheme.surfaceContainerLow,
      shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
    ) {
      val contentPadding = searchContentPadding()
      AnimatedContent(
        targetState = viewState,
        contentKey = { it::class },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "searchContent",
      ) { state ->
        when (state) {
          is BookSearchViewState.Idle -> IdleSearch(
            viewState = state,
            contentPadding = contentPadding,
            listener = listener,
          )
          is BookSearchViewState.Results -> SearchResults(
            viewState = state,
            contentPadding = contentPadding,
            listener = listener,
          )
          is BookSearchViewState.NoResults -> NoResults(
            viewState = state,
            contentPadding = contentPadding,
            onFoldersClick = listener::onFoldersClick,
            onSearchEverythingClick = listener::onSearchEverythingClick,
          )
        }
      }
    }
  }
}

@Composable
private fun searchContentPadding(): PaddingValues {
  val bottomInsets = WindowInsets.navigationBars.union(WindowInsets.ime).asPaddingValues()
  return PaddingValues(
    start = 12.dp,
    end = 12.dp,
    top = 16.dp,
    bottom = 16.dp + bottomInsets.calculateBottomPadding(),
  )
}

@Composable
private fun SearchField(
  query: TextFieldState,
  onSearch: () -> Unit,
  onBackClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val focusRequester = remember { FocusRequester() }
  // only when opening the search, not when coming back to the results from the player
  var focused by rememberSaveable { mutableStateOf(false) }
  LaunchedEffect(Unit) {
    if (!focused) {
      focusRequester.requestFocus()
      focused = true
    }
  }
  val shape = SearchBarDefaults.inputFieldShape
  val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
  SearchBarDefaults.InputField(
    textFieldState = query,
    // this screen is the expanded search, so the field never collapses
    searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Expanded),
    onSearch = { onSearch() },
    modifier = modifier
      .searchBarSharedBounds(shape)
      .fillMaxWidth()
      .focusRequester(focusRequester),
    placeholder = {
      Text(stringResource(StringsR.string.library_search_hint))
    },
    leadingIcon = {
      IconButton(onClick = onBackClick) {
        Icon(
          imageVector = VoiceIcons.ArrowBack,
          contentDescription = stringResource(StringsR.string.common_action_back),
        )
      }
    },
    trailingIcon = if (query.text.isNotEmpty()) {
      {
        IconButton(
          onClick = {
            query.clearText()
            focusRequester.requestFocus()
          },
        ) {
          Icon(
            imageVector = VoiceIcons.Close,
            contentDescription = stringResource(StringsR.string.library_search_clear),
          )
        }
      }
    } else {
      null
    },
    shape = shape,
    colors = SearchBarDefaults.inputFieldColors(
      focusedContainerColor = containerColor,
      unfocusedContainerColor = containerColor,
    ),
  )
}

@Composable
internal fun BookSearchField.label(): String {
  return stringResource(
    when (this) {
      BookSearchField.Title -> StringsR.string.library_search_field_titles
      BookSearchField.Author -> StringsR.string.library_search_field_authors
      BookSearchField.Series -> StringsR.string.library_search_field_series
      BookSearchField.Narrator -> StringsR.string.library_search_field_narrators
      BookSearchField.Genre -> StringsR.string.library_search_field_genres
    },
  )
}
