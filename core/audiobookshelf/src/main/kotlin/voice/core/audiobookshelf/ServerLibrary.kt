package voice.core.audiobookshelf

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import voice.core.audiobookshelf.download.AudiobookshelfDownloads
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.audiobookshelf.sync.AudiobookshelfSync
import voice.core.audiobookshelf.sync.ServerReachability
import voice.core.data.BookId
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository

/**
 * What the screens around the library need to know about the books on the server.
 */
interface ServerLibrary {

  /** The connected server as people know it, or null without one. */
  val serverName: Flow<String?>

  val syncing: Flow<Boolean>

  /** Server books that can't play right now: not downloaded while the server can't be reached. */
  val unavailableBooks: Flow<Set<BookId>>

  /** The server books that are downloaded, or on their way. */
  val downloadStates: Flow<Map<BookId, BookDownloadState>>

  suspend fun isConnected(): Boolean

  /** Syncs with the server, unless a sync is already running or there is no server. */
  fun sync()

  /** Downloads a server book, so it plays without a connection. */
  suspend fun download(bookId: BookId)

  /** Stops the download of a server book, or removes it. The book streams again. */
  suspend fun removeDownload(bookId: BookId)

  /** Whether to ask for notifications before a download, so its progress shows. Voice asks once. */
  suspend fun shouldAskForNotifications(): Boolean

  suspend fun onAskedForNotifications()
}

@ContributesBinding(AppScope::class)
@Inject
class AudiobookshelfServerLibrary internal constructor(
  private val audiobookshelf: Audiobookshelf,
  private val audiobookshelfSync: AudiobookshelfSync,
  private val downloads: AudiobookshelfDownloads,
  bookRepository: BookRepository,
) : ServerLibrary {

  override val serverName: Flow<String?> = audiobookshelf.connection.map { it?.serverName }.distinctUntilChanged()

  override val syncing: Flow<Boolean> = audiobookshelfSync.syncing

  override val unavailableBooks: Flow<Set<BookId>> = combine(
    audiobookshelfSync.reachability,
    downloads.states(),
    bookRepository.flow(),
  ) { reachability, downloadStates, books ->
    if (reachability != ServerReachability.Unreachable) return@combine emptySet()
    books
      .filter { it.id.isRemote && downloadStates[it.id] !is BookDownloadState.Downloaded }
      .map { it.id }
      .toSet()
  }.distinctUntilChanged()

  override val downloadStates: Flow<Map<BookId, BookDownloadState>> = downloads.states()

  override suspend fun isConnected(): Boolean = audiobookshelf.isConnected()

  override fun sync() {
    audiobookshelfSync.sync()
  }

  override suspend fun download(bookId: BookId) {
    downloads.download(bookId)
  }

  override suspend fun removeDownload(bookId: BookId) {
    downloads.remove(bookId)
  }

  override suspend fun shouldAskForNotifications(): Boolean = downloads.shouldAskForNotifications()

  override suspend fun onAskedForNotifications() {
    downloads.onAskedForNotifications()
  }
}
