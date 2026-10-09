package voice.core.audiobookshelf.download

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadManager
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.media3.exoplayer.offline.DownloadService
import androidx.media3.exoplayer.scheduler.Requirements
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.core.audiobookshelf.account.Preferences
import voice.core.audiobookshelf.account.PreferencesStore
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.core.logging.api.Logger
import java.util.concurrent.Executors
import kotlin.time.Duration.Companion.seconds

public sealed interface BookDownloadState {
  public data object NotDownloaded : BookDownloadState

  public data class Downloading(
    val progress: Float,
    /** The download waits for Wi-Fi, as downloading over mobile data is off. */
    val waitingForWifi: Boolean,
  ) : BookDownloadState

  public data object Downloaded : BookDownloadState

  public data object Failed : BookDownloadState
}

/**
 * Downloads server books so they play without a connection. Each audio file is a download of its own, tagged with
 * its book.
 */
@SingleIn(AppScope::class)
@Inject
public class AudiobookshelfDownloads internal constructor(
  private val context: Context,
  private val media: AudiobookshelfMedia,
  private val bookRepository: BookRepository,
  @PreferencesStore
  private val preferencesStore: DataStore<Preferences>,
) {

  private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
  private val downloads = MutableStateFlow<Map<String, Download>>(emptyMap())
  private val requirementsMet = MutableStateFlow(true)
  private var ticking = false

  internal val downloadManager: DownloadManager by lazy {
    DownloadManager(
      context,
      media.databaseProvider,
      media.cache,
      media.downloadDataSourceFactory,
      Executors.newFixedThreadPool(2),
    ).apply {
      maxParallelDownloads = 2
      addListener(
        object : DownloadManager.Listener {
          override fun onInitialized(downloadManager: DownloadManager) {
            refresh()
          }

          override fun onDownloadChanged(
            downloadManager: DownloadManager,
            download: Download,
            finalException: Exception?,
          ) {
            refresh()
          }

          override fun onDownloadRemoved(
            downloadManager: DownloadManager,
            download: Download,
          ) {
            refresh()
          }

          override fun onRequirementsStateChanged(
            downloadManager: DownloadManager,
            requirements: Requirements,
            notMetRequirements: Int,
          ) {
            requirementsMet.value = notMetRequirements == 0
            refresh()
          }
        },
      )
    }
  }

  internal fun start() {
    scope.launch {
      val manager = downloadManager
      manager.requirements = requirements(preferencesStore.data.first().downloadOverMobileData)
      refresh()
      // downloads that were cut off go on in the service, which keeps them alive in the background
      if (downloads.value.values.any { it.state != Download.STATE_COMPLETED && it.state != Download.STATE_FAILED }) {
        try {
          DownloadService.start(context, AudiobookshelfDownloadService::class.java)
        } catch (e: IllegalStateException) {
          Logger.w(e, "Could not resume the downloads")
        }
      }
    }
  }

  public fun state(bookId: BookId): Flow<BookDownloadState> = states().map { it[bookId] ?: BookDownloadState.NotDownloaded }
    .distinctUntilChanged()

  public fun states(): Flow<Map<BookId, BookDownloadState>> = downloads.map { downloads ->
    downloads.values
      .groupBy { BookId(it.request.data.decodeToString()) }
      .mapValues { (_, files) -> files.state(requirementsMet.value) }
  }.distinctUntilChanged()

  public val usedBytes: Flow<Long> = downloads.map { downloads ->
    downloads.values.sumOf { it.bytesDownloaded }
  }.distinctUntilChanged()

  public val downloadOverMobileData: Flow<Boolean> = preferencesStore.data.map { it.downloadOverMobileData }
    .distinctUntilChanged()

  public suspend fun setDownloadOverMobileData(enabled: Boolean) {
    preferencesStore.updateData { it.copy(downloadOverMobileData = enabled) }
    withContext(Dispatchers.Main) {
      DownloadService.sendSetRequirements(context, AudiobookshelfDownloadService::class.java, requirements(enabled), false)
    }
  }

  public suspend fun download(bookId: BookId) {
    if (!bookId.isRemote) return
    val book = bookRepository.get(bookId) ?: return
    withContext(Dispatchers.Main) {
      book.chapters.forEach { chapter ->
        val request = DownloadRequest.Builder(chapter.id.value, chapter.id.value.toUri())
          .setData(bookId.value.encodeToByteArray())
          .build()
        DownloadService.sendAddDownload(context, AudiobookshelfDownloadService::class.java, request, false)
      }
    }
  }

  public suspend fun remove(bookId: BookId) {
    val ids = downloads.value.values
      .filter { it.request.data.decodeToString() == bookId.value }
      .map { it.request.id }
    withContext(Dispatchers.Main) {
      ids.forEach { id ->
        DownloadService.sendRemoveDownload(context, AudiobookshelfDownloadService::class.java, id, false)
      }
    }
  }

  public suspend fun removeAll() {
    withContext(Dispatchers.Main) {
      DownloadService.sendRemoveAllDownloads(context, AudiobookshelfDownloadService::class.java, false)
    }
  }

  private fun refresh() {
    val manager = downloadManager
    val all = buildMap {
      manager.downloadIndex.getDownloads().use { cursor ->
        while (cursor.moveToNext()) {
          put(cursor.download.request.id, cursor.download)
        }
      }
      // the index only updates on state changes, the current downloads also know their progress
      manager.currentDownloads.forEach { put(it.request.id, it) }
    }
    downloads.value = all
    keepTickingWhileDownloading()
  }

  private fun keepTickingWhileDownloading() {
    if (ticking || downloads.value.values.none { it.state == Download.STATE_DOWNLOADING }) return
    ticking = true
    scope.launch {
      while (downloads.value.values.any { it.state == Download.STATE_DOWNLOADING }) {
        delay(1.seconds)
        val current = downloadManager.currentDownloads.associateBy { it.request.id }
        downloads.value = downloads.value + current
      }
      ticking = false
    }
  }
}

private fun requirements(overMobileData: Boolean): Requirements {
  return Requirements(if (overMobileData) Requirements.NETWORK else Requirements.NETWORK_UNMETERED)
}

private fun List<Download>.state(requirementsMet: Boolean): BookDownloadState {
  if (all { it.state == Download.STATE_COMPLETED }) return BookDownloadState.Downloaded
  if (any { it.state == Download.STATE_FAILED }) return BookDownloadState.Failed
  val totalBytes = sumOf { download ->
    when {
      download.contentLength > 0 -> download.contentLength
      else -> 0L
    }
  }
  val progress = if (totalBytes > 0) {
    sumOf { it.bytesDownloaded }.toFloat() / totalBytes
  } else {
    map { it.percentDownloaded.coerceAtLeast(0F) / 100F }.average().toFloat()
  }
  val waiting = !requirementsMet || any { it.stopReason != Download.STOP_REASON_NONE }
  return BookDownloadState.Downloading(progress = progress.coerceIn(0F, 1F), waitingForWifi = waiting)
}
