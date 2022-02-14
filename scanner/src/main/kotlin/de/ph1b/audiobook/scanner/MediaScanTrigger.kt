package de.ph1b.audiobook.scanner

import android.content.Context
import android.net.Uri
import androidx.datastore.core.DataStore
import androidx.documentfile.provider.DocumentFile
import de.ph1b.audiobook.common.pref.AudiobookFolders
import de.ph1b.audiobook.data.repo.BookRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MediaScanTrigger
@Inject constructor(
  @AudiobookFolders
  private val audiobookFolders: DataStore<List<@JvmSuppressWildcards Uri>>,
  private val scanner: MediaScanner,
  private val context: Context,
  private val coverScanner: CoverScanner,
  private val bookRepo: BookRepository,
) {

  private val _scannerActive = MutableStateFlow(false)
  val scannerActive: Flow<Boolean> = _scannerActive

  private val scope = CoroutineScope(Dispatchers.IO)
  private var scanningJob: Job? = null

  fun scan(restartIfScanning: Boolean = false) {
    Timber.i("scanForFiles with restartIfScanning=$restartIfScanning")
    if (scanningJob?.isActive == true && !restartIfScanning) {
      return
    }
    val oldJob = scanningJob
    scanningJob = scope.launch {
      _scannerActive.value = true
      oldJob?.cancelAndJoin()

      val folders = audiobookFolders.data.first()
        .mapNotNull { DocumentFile.fromTreeUri(context, it) }
      scanner.scan(folders)

      val books = bookRepo.flow().first()
      coverScanner.scan(books)

      _scannerActive.value = false
    }
  }
}
