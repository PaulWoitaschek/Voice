package voice.features.folderPicker.selectType

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateSetOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.documentfile.provider.DocumentFile
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withContext
import voice.core.common.DispatcherProvider
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.documentfile.nameWithoutExtension
import voice.core.logging.api.Logger
import voice.core.scanner.BookPreview
import voice.core.scanner.BookPreviewer
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin

@AssistedInject
class SelectFolderTypeViewModel(
  private val dispatcherProvider: DispatcherProvider,
  private val audiobookFolders: AudiobookFolders,
  private val navigator: Navigator,
  private val documentFileFactory: CachedDocumentFileFactory,
  private val bookPreviewer: BookPreviewer,
  @Assisted
  private val uri: Uri,
  @Assisted
  private val documentFile: DocumentFile,
  @Assisted
  private val origin: Origin,
  @Assisted
  private val currentType: FolderType?,
) {

  private val currentMode = currentType?.toFolderMode()

  // until the user picks one, this is the current mode or the guess
  private val selectedMode = mutableStateOf(currentMode)

  // what the library will show for the analyzed books, by their uri. Books without playable files map to null.
  private val previews = mutableStateMapOf<Uri, BookPreview?>()

  // books whose analysis failed keep showing what their files tell
  private val failedPreviews = mutableStateSetOf<Uri>()

  internal fun selectMode(mode: FolderMode) {
    selectedMode.value = mode
  }

  internal fun onCloseClick() {
    navigator.goBack()
  }

  internal fun add() {
    val mode = selectedMode.value ?: return
    if (currentType != null) {
      if (mode != currentMode) {
        audiobookFolders.add(uri, mode.toFolderType())
      }
      navigator.goBack()
      return
    }
    audiobookFolders.add(uri, mode.toFolderType())
    when (origin) {
      Origin.Default -> {
        navigator.setRoot(Destination.BookOverview)
      }
      Origin.Onboarding -> {
        navigator.goTo(Destination.OnboardingCompletion)
      }
    }
  }

  @Composable
  internal fun viewState(): SelectFolderTypeViewState {
    val folder: CachedDocumentFile = remember {
      documentFileFactory.create(documentFile.uri)
    }
    val folderName by produceState(initialValue = "") {
      value = withContext(dispatcherProvider.io) { folder.nameWithoutExtension() }
    }
    val structure by produceState<FolderStructure?>(initialValue = null) {
      val loaded = withContext(dispatcherProvider.io) {
        FolderStructure(
          guess = folder.guessFolderMode(),
          books = FolderMode.entries.associateWith { folder.books(it) },
        )
      }
      if (selectedMode.value == null) {
        selectedMode.value = loaded.guess
      }
      value = loaded
    }
    val loadedStructure = structure
    val selectedMode = selectedMode.value ?: FolderMode.SingleBook
    val selectedBooks = loadedStructure?.books?.get(selectedMode).orEmpty()
    LaunchedEffect(selectedBooks) {
      analyze(selectedBooks)
    }
    // the books are analyzed from the top, so this is the one in the works
    val analyzing = selectedBooks.firstOrNull { !it.isAnalyzed() }?.file?.uri
    return SelectFolderTypeViewState(
      folderName = folderName,
      loading = loadedStructure == null,
      selectedMode = selectedMode,
      guessedMode = loadedStructure?.guess,
      books = selectedBooks.withPreviews(analyzing),
      options = FolderMode.entries.map { mode ->
        SelectFolderTypeViewState.Option(
          mode = mode,
          books = loadedStructure?.books?.get(mode).orEmpty().withPreviews(analyzing),
        )
      },
      editing = currentType != null,
      onboarding = origin == Origin.Onboarding && currentType == null,
    )
  }

  // one book after the other, so the books on top fill in first
  private suspend fun analyze(books: List<FolderBook>) {
    books.forEach { book ->
      if (book.isAnalyzed()) return@forEach
      val uri = book.file.uri
      try {
        previews[uri] = withContext(dispatcherProvider.io) {
          bookPreviewer.preview(book.file)
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        Logger.w(e, "Could not analyze ${book.file}")
        failedPreviews += uri
      }
    }
  }

  private fun FolderBook.isAnalyzed(): Boolean = file.uri in previews || file.uri in failedPreviews

  // books without playable files are left out, the scan skips them as well
  private fun List<FolderBook>.withPreviews(analyzing: Uri?): List<SelectFolderTypeViewState.Book> {
    return mapNotNull { folderBook ->
      val uri = folderBook.file.uri
      if (uri in previews) {
        val preview = previews[uri] ?: return@mapNotNull null
        folderBook.book.copy(name = preview.name, author = preview.author, duration = preview.duration)
      } else {
        folderBook.book.copy(analyzing = uri == analyzing)
      }
    }
  }

  private class FolderStructure(
    val guess: FolderMode,
    val books: Map<FolderMode, List<FolderBook>>,
  )

  @AssistedFactory
  interface Factory {
    fun create(
      uri: Uri,
      documentFile: DocumentFile,
      origin: Origin,
      currentType: FolderType?,
    ): SelectFolderTypeViewModel
  }
}
