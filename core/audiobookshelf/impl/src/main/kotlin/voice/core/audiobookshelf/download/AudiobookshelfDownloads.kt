package voice.core.audiobookshelf.download

import android.app.Notification
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.core.audiobookshelf.ServerDownloads
import voice.core.audiobookshelf.account.PreferencesStore
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger

/**
 * Downloads server books so they play without a connection. Each audio file is a download of its own, tagged with its book. While books
 * download, a notification shows their progress and lets the listener stop them.
 */
@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
public class AudiobookshelfDownloads
internal constructor(
  private val context: Context,
  private val media: AudiobookshelfMedia,
  private val bookRepository: BookRepository,
  private val preferencesStore: PreferencesStore,
  private val notifications: DownloadNotifications,
) : ServerDownloads {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val files = MutableStateFlow<Map<String, FileDownload>>(emptyMap())
  private val notMetRequirements = MutableStateFlow(0)
  private val initialized = MutableStateFlow(false)
  private var ticking = false
  private var appVisible = false

  internal val downloadManager: DownloadManager by lazy {
    DownloadManager(
        context,
        media.databaseProvider,
        media.cache,
        media.downloadDataSourceFactory,
        Executors.newFixedThreadPool(2),
      )
      .apply {
        maxParallelDownloads = 2
        addListener(
          object : DownloadManager.Listener {
            override fun onInitialized(downloadManager: DownloadManager) {
              scope.launch {
                val stored = withContext(Dispatchers.IO) { storedDownloads(downloadManager) }
                // what changed while the index was read is newer
                files.update { stored + it }
                initialized.value = true
                keepTickingWhileDownloading()
              }
            }

            override fun onDownloadChanged(
              downloadManager: DownloadManager,
              download: Download,
              finalException: Exception?,
            ) {
              if (finalException != null) Logger.w(finalException, "A download failed")
              files.update { it + (download.request.id to download.snapshot()) }
              keepTickingWhileDownloading()
            }

            override fun onDownloadRemoved(
              downloadManager: DownloadManager,
              download: Download,
            ) {
              files.update { it - download.request.id }
            }

            override fun onRequirementsStateChanged(
              downloadManager: DownloadManager,
              requirements: Requirements,
              notMetRequirements: Int,
            ) {
              this@AudiobookshelfDownloads.notMetRequirements.value = notMetRequirements
            }
          },
        )
      }
  }

  private val tracked: StateFlow<Map<BookId, BookDownload>> =
    combine(
        files,
        notMetRequirements,
        bookRepository.flow(),
      ) { files, notMetRequirements, books ->
        val waitingFor =
          when {
            notMetRequirements and Requirements.NETWORK_UNMETERED != 0 -> WaitingFor.Wifi
            notMetRequirements and Requirements.NETWORK != 0 -> WaitingFor.Connection
            else -> null
          }
        books
          .filter { it.id.isRemote }
          .mapNotNull { book ->
            val state = bookDownloadState(book.chapters, files, waitingFor) ?: return@mapNotNull null
            book.id to BookDownload(book, state)
          }
          .toMap()
      }
      .stateIn(scope, SharingStarted.Eagerly, emptyMap())

  // the notification channel comes with the first download, as long as there is none it stays out of the settings
  internal fun start() {
    scope.launch {
      val manager = downloadManager
      manager.requirements = requirements(preferencesStore.data.first().downloadOverMobileData)
      notMetRequirements.value = manager.notMetRequirements
    }
    notifyResults()
  }

  internal fun onAppVisible() {
    appVisible = true
    // downloads that were cut off go on, which may only start while Voice is visible
    scope.launch {
      initialized.first { it }
      if (files.value.values.any { it.state.isActive() }) startService()
    }
  }

  internal fun onAppHidden() {
    appVisible = false
  }

  override val states: Flow<Map<BookId, BookDownloadState>> =
    tracked.map { tracked -> tracked.mapValues { it.value.state } }.distinctUntilChanged()

  override val books: Flow<List<BookDownload>> =
    tracked
      .map { tracked ->
        tracked.values.sortedBy { it.book.name.lowercase() }
      }
      .distinctUntilChanged()

  override val usedBytes: Flow<Long> =
    files
      .map { files ->
        files.values.sumOf { it.bytesDownloaded }
      }
      .distinctUntilChanged()

  override val downloadOverMobileData: Flow<Boolean> = preferencesStore.data.map { it.downloadOverMobileData }.distinctUntilChanged()

  override suspend fun setDownloadOverMobileData(enabled: Boolean) {
    preferencesStore.updateData { it.copy(downloadOverMobileData = enabled) }
    withContext(Dispatchers.Main) {
      downloadManager.requirements = requirements(enabled)
    }
  }

  override suspend fun shouldAskForNotifications(): Boolean = !preferencesStore.data.first().askedForNotifications

  override suspend fun onAskedForNotifications() {
    preferencesStore.updateData { it.copy(askedForNotifications = true) }
  }

  override suspend fun download(bookId: BookId) {
    if (addMissingFiles(bookId)) startService()
  }

  override suspend fun remove(bookId: BookId) {
    remove(setOf(bookId))
  }

  private suspend fun remove(bookIds: Set<BookId>) {
    withContext(Dispatchers.Main) {
      files.value.filterValues { it.bookId in bookIds }.keys.forEach { downloadManager.removeDownload(it) }
    }
    bookIds.forEach(notifications::cancelResult)
  }

  override suspend fun removeAll() {
    withContext(Dispatchers.Main) {
      downloadManager.removeAllDownloads()
    }
    notifications.cancelResults()
  }

  /**
   * Removes the downloads of files that are no longer part of a book on the server, and downloads the new files of books that were
   * downloaded.
   */
  internal suspend fun reconcile() {
    val books = bookRepository.all().filter { it.id.isRemote }
    val current = books.flatMap { book -> book.chapters.map { it.id.value } }.toSet()
    val present = files.value
    // by its book, as the server can replace all files of a book at once
    val downloadedBooks = present.values.filter { it.state != Download.STATE_REMOVING }.mapNotNull { it.bookId }.toSet()
    withContext(Dispatchers.Main) {
      (present.keys - current).forEach { downloadManager.removeDownload(it) }
    }
    val added =
      books
        .filter { book -> book.id in downloadedBooks && book.chapters.any { present[it.id.value] == null } }
        .map { addMissingFiles(it.id) }
    if (added.any { it } && appVisible) startService()
  }

  internal fun onNotificationAction(
    intent: Intent,
    onDone: () -> Unit,
  ) {
    scope.launch {
      try {
        val bookIds = intent.bookIds()
        when (intent.action) {
          DownloadActionReceiver.ACTION_STOP -> remove(bookIds)
          DownloadActionReceiver.ACTION_RETRY -> bookIds.forEach { download(it) }
        }
      } finally {
        onDone()
      }
    }
  }

  /** The notification while books download, with the state Voice knows right now. */
  internal fun foregroundNotification(current: List<Download>): Notification {
    val bookIds = current.filter { it.state.isActive() }.mapNotNull { it.bookId() }.toSet()
    return notifications.progress(activeDownloads(bookIds))
  }

  internal fun hasActiveDownloads(): Boolean = downloadManager.currentDownloads.any { it.state.isActive() }

  /** Changes of what the notification shows while books download. */
  internal val foregroundNotificationChanges: Flow<Unit> =
    tracked.map { tracked -> activeDownloads(tracked.keys).map { it.book.name to it.state } }.distinctUntilChanged().map {}

  private fun activeDownloads(bookIds: Set<BookId>): List<ActiveDownload> {
    val tracked = tracked.value
    return bookIds.mapNotNull { bookId ->
      val download = tracked[bookId] ?: return@mapNotNull null
      val state = download.state as? BookDownloadState.Downloading ?: return@mapNotNull null
      ActiveDownload(download.book, state)
    }
  }

  /** @return whether a file was added */
  private suspend fun addMissingFiles(bookId: BookId): Boolean {
    if (!bookId.isRemote) return false
    val book = bookRepository.get(bookId) ?: return false
    return withContext(Dispatchers.Main) {
      val present = files.value
      val missing = book.chapters.filter { present[it.id.value]?.state != Download.STATE_COMPLETED }
      missing.forEach { chapter ->
        val request = DownloadRequest.Builder(chapter.id.value, chapter.id.value.toUri()).setData(bookId.value.encodeToByteArray()).build()
        downloadManager.addDownload(request)
      }
      notifications.cancelResult(bookId)
      missing.isNotEmpty()
    }
  }

  // the service keeps the downloads alive in the background and shows their notification
  private fun startService() {
    try {
      DownloadService.startForeground(context, AudiobookshelfDownloadService::class.java)
    } catch (e: IllegalStateException) {
      // in the background Android doesn't allow it, the downloads go on once Voice is visible
      Logger.w(e, "Could not start the downloads")
    }
  }

  // tells about books that finished while the listener was elsewhere, Voice itself shows it on the cover
  private fun notifyResults() {
    scope.launch {
      var previous: Map<BookId, BookDownload>? = null
      tracked.collect { current ->
        val before = previous
        previous = current
        if (before == null) return@collect
        current.forEach { (bookId, download) ->
          val wasDownloading = before[bookId]?.state is BookDownloadState.Downloading
          when (download.state) {
            is BookDownloadState.Downloaded -> if (wasDownloading && !appVisible) notifications.downloaded(download.book)
            BookDownloadState.Failed -> if (wasDownloading && !appVisible) notifications.failed(download.book)
            is BookDownloadState.Downloading,
            BookDownloadState.NotDownloaded -> Unit
          }
        }
      }
    }
  }

  private fun storedDownloads(manager: DownloadManager): Map<String, FileDownload> = buildMap {
    manager.downloadIndex.getDownloads().use { cursor ->
      while (cursor.moveToNext()) {
        put(cursor.download.request.id, cursor.download.snapshot())
      }
    }
  }

  private fun keepTickingWhileDownloading() {
    if (ticking || files.value.values.none { it.state == Download.STATE_DOWNLOADING }) return
    ticking = true
    scope.launch {
      while (files.value.values.any { it.state == Download.STATE_DOWNLOADING }) {
        delay(1.seconds)
        val current = downloadManager.currentDownloads.associate { it.request.id to it.snapshot() }
        files.value += current
      }
      ticking = false
    }
  }
}

private fun Download.bookId(): BookId? = request.data.takeIf { it.isNotEmpty() }?.decodeToString()?.let(::BookId)

// the progress of a running download changes in place, a snapshot can tell it apart from before
private fun Download.snapshot(): FileDownload =
  FileDownload(
    bookId = bookId(),
    state = state,
    bytesDownloaded = bytesDownloaded,
  )

private fun requirements(overMobileData: Boolean): Requirements {
  return Requirements(if (overMobileData) Requirements.NETWORK else Requirements.NETWORK_UNMETERED)
}
