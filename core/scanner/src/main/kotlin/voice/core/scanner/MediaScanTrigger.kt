package voice.core.scanner

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.measureTime
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
import voice.core.data.repo.BookRepository
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.CachedDocumentFileFactory
import voice.core.logging.api.Logger

@SingleIn(AppScope::class)
@Inject
public class MediaScanTrigger
internal constructor(
  private val audiobookFolders: AudiobookFolders,
  private val scanner: MediaScanner,
  private val coverScanner: CoverScanner,
  private val bookRepo: BookRepository,
  private val documentFileFactory: CachedDocumentFileFactory,
  dispatcherProvider: DispatcherProvider,
) {

  public val scannerActive: Flow<Boolean>
    field = MutableStateFlow(false)

  private val scope = CoroutineScope(dispatcherProvider.io)
  private var scanningJob: Job? = null

  public fun scan(restartIfScanning: Boolean = false) {
    Logger.i("scanForFiles with restartIfScanning=$restartIfScanning")
    // looking for covers can take long, so a request then starts over instead of missing new books
    if (scannerActive.value && !restartIfScanning) {
      return
    }
    scannerActive.value = true
    val oldJob = scanningJob
    scanningJob = scope.launch {
      oldJob?.cancelAndJoin()

      measureTime {
        val folders: Map<FolderType, List<CachedDocumentFile>> =
          audiobookFolders.all().first().mapValues { (_, documentFilesWithUri) ->
            documentFilesWithUri.map {
              documentFileFactory.create(it.documentFile.uri)
            }
          }
        scanner.scan(folders)
      }
        .also {
          Logger.i("scan took $it")
        }
      scannerActive.value = false

      val books = bookRepo.all()
      coverScanner.scan(books)
    }
  }
}
