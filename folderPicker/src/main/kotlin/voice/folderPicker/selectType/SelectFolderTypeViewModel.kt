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
import kotlinx.coroutines.withContext
import voice.common.DispatcherProvider
import voice.common.navigation.Navigator
import voice.data.folders.AudiobookFolders
import voice.data.folders.FolderType
import javax.inject.Inject

class SelectFolderTypeViewModel
@Inject constructor(
  private val dispatcherProvider: DispatcherProvider,
  private val audiobookFolders: AudiobookFolders,
  private val navigator: Navigator,
) {

  internal lateinit var args: Args

  private var selectedFolderMode: MutableState<FolderMode?> = mutableStateOf(null)

  private fun DocumentFileCache.CachedDocumentFile.defaultFolderMode(): FolderMode {
    return when {
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
      uri = args.uri,
      type = when (selectedFolderMode.value) {
        FolderMode.Audiobooks -> FolderType.Root
        FolderMode.SingleBook -> FolderType.SingleFolder
        null -> error("Add should not be clickable at this point")
      },
    )
    navigator.goBack()
  }

  @Composable
  internal fun viewState(): SelectFolderTypeViewState {
    val documentFile: DocumentFileCache.CachedDocumentFile = remember {
      with(DocumentFileCache()) {
        args.documentFile.cached()
      }
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

  data class Args(
    val uri: Uri,
    val documentFile: DocumentFile,
  )
}

private fun DocumentFileCache.CachedDocumentFile.nameWithoutExtension(): String {
  val name = name ?: return ""
  return name.substringBeforeLast(".")
    .takeUnless { it.isBlank() }
    ?: name
}
