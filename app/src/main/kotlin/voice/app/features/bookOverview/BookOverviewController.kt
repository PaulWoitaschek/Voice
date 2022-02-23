package voice.app.features.bookOverview

import android.content.Intent
import android.view.View
import androidx.compose.runtime.Composable
import androidx.datastore.core.DataStore
import kotlinx.coroutines.launch
import voice.app.features.GalleryPicker
import voice.app.features.imagepicker.CoverFromInternetController
import voice.app.injection.appComponent
import voice.app.misc.conductor.asTransaction
import voice.bookOverview.BookOverviewViewModel
import voice.bookOverview.views.BookOverview
import voice.common.compose.ComposeController
import voice.common.pref.CurrentBook
import voice.data.Book
import voice.folderPicker.FolderPickerController
import voice.playbackScreen.BookPlayController
import voice.settings.SettingsController
import javax.inject.Inject

class BookOverviewController : ComposeController(), EditBookBottomSheetController.Callback {

  init {
    appComponent.inject(this)
  }

  @field:[Inject CurrentBook]
  lateinit var currentBookIdPref: DataStore<Book.Id?>

  @Inject
  lateinit var viewModel: BookOverviewViewModel

  @Inject
  lateinit var galleryPicker: GalleryPicker

  @Composable
  override fun Content() {
    val viewState = viewModel.state()
    BookOverview(
      viewState = viewState,
      onLayoutIconClick = viewModel::toggleGrid,
      onSettingsClick = ::toSettings,
      onBookClick = ::toBook,
      onBookFolderClick = ::toFolderOverview,
      onPlayButtonClick = viewModel::playPause
    )
  }

  private fun toSettings() {
    router.pushController(SettingsController().asTransaction())
  }

  private fun toFolderOverview() {
    val controller = FolderPickerController()
    router.pushController(controller.asTransaction())
  }

  override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
    val arguments = galleryPicker.parse(requestCode, resultCode, data)
    if (arguments != null) {
      EditCoverDialogController(arguments).showDialog(router)
    }
  }

  private fun toBook(bookId: Book.Id) {
    lifecycleScope.launch {
      currentBookIdPref.updateData { bookId }
      router.pushController(BookPlayController(bookId).asTransaction())
    }
  }


  override fun onInternetCoverRequested(book: Book.Id) {
    router.pushController(
      CoverFromInternetController(book)
        .asTransaction()
    )
  }

  override fun onFileCoverRequested(book: Book.Id) {
    galleryPicker.pick(book, this)
  }

  override fun onAttach(view: View) {
    viewModel.attach()
  }
}
