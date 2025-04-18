package voice.folderPicker.selectType

import android.net.Uri
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.documentfile.provider.DocumentFile
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.withContext
import voice.common.DispatcherProvider
import voice.common.navigation.Destination
import voice.common.navigation.Destination.SelectFolderType.Mode
import voice.common.navigation.Navigator
import voice.data.audioFileCount
import voice.data.folders.AudiobookFolders
import voice.data.folders.FolderType
import voice.data.isAudioFile
import voice.documentfile.CachedDocumentFile
import voice.documentfile.CachedDocumentFileFactory
import voice.documentfile.nameWithoutExtension
import com.kiwi.navigationcompose.typed.navigate as typedNavigate

class SelectFolderTypeViewModel
@AssistedInject constructor(
  private val dispatcherProvider: DispatcherProvider,
  private val audiobookFolders: AudiobookFolders,
  private val navigator: Navigator,
  private val documentFileFactory: CachedDocumentFileFactory,
  @Assisted
  private val uri: Uri,
  @Assisted
  private val documentFile: DocumentFile,
  @Assisted
  private val mode: Mode,
) {

  private var selectedFolderMode: MutableState<FolderMode?> = mutableStateOf(null)

  private fun CachedDocumentFile.defaultFolderMode(): FolderMode {
    return when {
      name in listOf("Audiobooks", "Hörbücher") -> FolderMode.Audiobooks
      children.any { it.isAudioFile() } && children.any { it.isDirectory } -> {
        FolderMode.Audiobooks
      }
      children.any {
        val fileIsAudiobookThresholdMb = 200
        it.isAudioFile() && it.length > fileIsAudiobookThresholdMb * 1_000_000
      } -> {
        FolderMode.Audiobooks
      }
      else -> FolderMode.SingleBook
    }
  }

  internal fun setFolderMode(folderMode: FolderMode) {
    selectedFolderMode.value = folderMode
  }

  internal fun onCloseClick() {
    navigator.goBack()
  }

  internal fun add() {
    audiobookFolders.add(
      uri = uri,
      type = when (selectedFolderMode.value) {
        FolderMode.Audiobooks -> FolderType.Root
        FolderMode.SingleBook -> FolderType.SingleFolder
        FolderMode.Authors -> FolderType.Author
        null -> error("Add should not be clickable at this point")
      },
    )
    when (mode) {
      Mode.Default -> {
        navigator.execute { navController ->
          navController.typedNavigate(Destination.BookOverview) {
            popUpTo(0)
          }
        }
      }
      Mode.Onboarding -> {
        navigator.goTo(Destination.OnboardingCompletion)
      }
    }
  }

  @Composable
  internal fun viewState(): SelectFolderTypeViewState {
    val documentFile: CachedDocumentFile = remember {
      documentFileFactory.create(documentFile.uri)
    }
    val selectedFolderMode = selectedFolderMode.value ?: documentFile.defaultFolderMode().also {
      selectedFolderMode.value = it
    }
    var books: List<SelectFolderTypeViewState.Book> by remember {
      mutableStateOf(emptyList())
    }
    var loading by remember { mutableStateOf(true) }
    LaunchedEffect(selectedFolderMode) {
      loading = true
      withContext(dispatcherProvider.io) {
        books = when (selectedFolderMode) {
          FolderMode.Audiobooks -> {
            documentFile.children.map { child ->
              SelectFolderTypeViewState.Book(
                name = child.nameWithoutExtension(),
                fileCount = child.audioFileCount(),
              )
            }
          }
          FolderMode.SingleBook -> {
            listOf(
              SelectFolderTypeViewState.Book(
                name = documentFile.nameWithoutExtension(),
                fileCount = documentFile.audioFileCount(),
              ),
            )
          }
          FolderMode.Authors -> {
            documentFile.children.flatMap { author ->
              val authorName = author.nameWithoutExtension()
              if (author.isAudioFile()) {
                listOf(
                  SelectFolderTypeViewState.Book(
                    name = author.nameWithoutExtension(),
                    fileCount = author.audioFileCount(),
                  ),
                )
              } else {
                author.children.map { child ->
                  SelectFolderTypeViewState.Book(
                    name = "${child.nameWithoutExtension()} ($authorName)",
                    fileCount = child.audioFileCount(),
                  )
                }
              }
            }
          }
        }
        loading = false
      }
    }
    return SelectFolderTypeViewState(
      books = books,
      selectedFolderMode = selectedFolderMode,
      loading = loading,
      noBooksDetected = !loading && books.isEmpty(),
      addButtonVisible = books.isNotEmpty(),
    )
  }

  @AssistedFactory
  interface Factory {
    fun create(
      uri: Uri,
      documentFile: DocumentFile,
      mode: Mode,
    ): SelectFolderTypeViewModel
  }
}
