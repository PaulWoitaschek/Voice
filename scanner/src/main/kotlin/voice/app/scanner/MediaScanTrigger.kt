package voice.app.scanner

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.data.folders.AudiobookFolders
import voice.data.folders.FolderType
import voice.data.repo.BookRepository
import voice.documentfile.CachedDocumentFile
import voice.documentfile.CachedDocumentFileFactory
import voice.logging.core.Logger
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.measureTime

@Singleton
class MediaScanTrigger
@Inject constructor(
  private val audiobookFolders: AudiobookFolders,
  private val scanner: MediaScanner,
  private val coverScanner: CoverScanner,
  private val bookRepo: BookRepository,
  private val documentFileFactory: CachedDocumentFileFactory,
) {

  private val _scannerActive = MutableStateFlow(false)
  val scannerActive: Flow<Boolean> = _scannerActive

  private val scope = CoroutineScope(Dispatchers.IO)
  private var scanningJob: Job? = null

  fun scan(restartIfScanning: Boolean = false) {
    Logger.i("scanForFiles with restartIfScanning=$restartIfScanning")
    if (scanningJob?.isActive == true && !restartIfScanning) {
      return
    }
    val oldJob = scanningJob
    scanningJob = scope.launch {
      _scannerActive.value = true
      oldJob?.cancelAndJoin()

      measureTime {
        val folders: Map<FolderType, List<CachedDocumentFile>> = audiobookFolders.all()
          .first()
          .mapValues { (_, documentFilesWithUri) ->
            documentFilesWithUri.map {
              documentFileFactory.create(it.documentFile.uri)
            }
          }
        scanner.scan(folders)
      }.also {
        Logger.i("scan took $it")
      }
      _scannerActive.value = false

      val books = bookRepo.all()
      coverScanner.scan(books)
    }
  }
}
