package voice.core.audiobookshelf.download

import androidx.media3.exoplayer.offline.Download
import voice.core.data.BookId
import voice.core.data.Chapter

public sealed interface BookDownloadState {
  public data object NotDownloaded : BookDownloadState

  public data class Downloading(
    val progress: Float,
    val downloadedBytes: Long,
    /** The size of the whole book, or 0 when the server didn't tell. */
    val totalBytes: Long,
    /** Why the download doesn't go on right now, or null while it does. */
    val waitingFor: WaitingFor?,
  ) : BookDownloadState

  public data class Downloaded(val bytes: Long) : BookDownloadState

  /** The download gave up after an error and waits to be tried again. */
  public data object Failed : BookDownloadState
}

public enum class WaitingFor {
  /** Downloading over mobile data is off. */
  Wifi,
  Connection,
}

internal fun Int.isActive(): Boolean {
  return this == Download.STATE_QUEUED || this == Download.STATE_DOWNLOADING || this == Download.STATE_RESTARTING
}

/** A download of one audio file of a book, as Voice last saw it. */
internal data class FileDownload(
  val bookId: BookId?,
  @param:Download.State
  val state: Int,
  val bytesDownloaded: Long,
)

/**
 * The download state of a book, judged by the files it has now. A book that got new files on the server isn't
 * downloaded anymore until those are. Null when none of its files are downloaded.
 */
internal fun bookDownloadState(
  chapters: List<Chapter>,
  files: Map<String, FileDownload>,
  waitingFor: WaitingFor?,
): BookDownloadState? {
  // a stopped download is on its way out
  val tracked = chapters.map { chapter -> chapter to files[chapter.id.value]?.takeUnless { it.state == Download.STATE_REMOVING } }
  if (tracked.all { it.second == null }) return null
  if (tracked.all { it.second?.state == Download.STATE_COMPLETED }) {
    return BookDownloadState.Downloaded(bytes = tracked.sumOf { it.second!!.bytesDownloaded })
  }
  // the other files go on after one failed, the book only failed once they are done
  val active = tracked.any { (_, file) -> file != null && file.state.isActive() }
  if (!active && tracked.any { it.second?.state == Download.STATE_FAILED }) return BookDownloadState.Failed

  val downloadedBytes = tracked.sumOf { it.second?.bytesDownloaded ?: 0L }
  val sizesKnown = chapters.all { it.fileSize > 0 }
  val totalBytes = if (sizesKnown) chapters.sumOf { it.fileSize } else 0L
  val progress = if (sizesKnown) {
    downloadedBytes.toFloat() / totalBytes
  } else {
    tracked.count { it.second?.state == Download.STATE_COMPLETED }.toFloat() / chapters.size
  }
  return BookDownloadState.Downloading(
    progress = progress.coerceIn(0F, 1F),
    downloadedBytes = downloadedBytes,
    totalBytes = totalBytes,
    waitingFor = waitingFor,
  )
}
