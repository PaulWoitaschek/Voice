package voice.core.audiobookshelf.download

import voice.core.data.Book

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

/** A server book with a download, and how far it got. */
public data class BookDownload(
  val book: Book,
  val state: BookDownloadState,
)
