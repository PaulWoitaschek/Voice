package voice.features.bookmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface AudiologProvider {

  @Provides
  @IntoSet
  fun audiologNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.Audiolog> { key ->
    NavEntry(key) {
      AudiologScreen(bookId = key.bookId)
    }
  }
}

@Composable
fun AudiologScreen(bookId: BookId) {
  val viewModel = retain(bookId.value) {
    rootGraphAs<Graph>().bookmarkViewModelFactory.create(bookId, audiolog = true)
  }
  val viewState = viewModel.viewState()
  BookmarkScreen(
    viewState = viewState,
    titleRes = StringsR.string.audiolog,
    showAddButton = false,
    audiolog = true,
    onClose = viewModel::closeScreen,
    onAdd = viewModel::onAddClick,
    onDelete = viewModel::deleteBookmark,
    onEdit = viewModel::onEditClick,
    onScrollConfirm = viewModel::onScrollConfirm,
    onClick = viewModel::selectBookmark,
    onNewBookmarkNameChoose = viewModel::addBookmark,
    onCloseDialog = viewModel::closeDialog,
    onEditBookmark = viewModel::editBookmark,
  )
}
