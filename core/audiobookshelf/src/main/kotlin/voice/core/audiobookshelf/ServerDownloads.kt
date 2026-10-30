package voice.core.audiobookshelf

import kotlinx.coroutines.flow.Flow
import voice.core.audiobookshelf.download.BookDownload
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.data.BookId

interface ServerDownloads {

  /** The download state of the server books that are downloaded, or on their way. */
  val states: Flow<Map<BookId, BookDownloadState>>

  /** The server books that are downloaded, or on their way, by name. */
  val books: Flow<List<BookDownload>>

  val usedBytes: Flow<Long>

  val downloadOverMobileData: Flow<Boolean>

  suspend fun setDownloadOverMobileData(enabled: Boolean)

  /** Downloads the files of a server book that aren't on the device yet, so it plays without a connection. */
  suspend fun download(bookId: BookId)

  /** Stops the download of a server book, or removes it. The book streams again. */
  suspend fun remove(bookId: BookId)

  suspend fun removeAll()

  /** Whether to ask for notifications before a download, so its progress shows. Voice asks once. */
  suspend fun shouldAskForNotifications(): Boolean

  suspend fun onAskedForNotifications()
}
