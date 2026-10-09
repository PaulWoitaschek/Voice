package voice.core.audiobookshelf.sync

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import retrofit2.Response
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.SyncedBookmarksStore
import voice.core.audiobookshelf.api.AbsBookmark
import voice.core.audiobookshelf.api.AudiobookshelfApi
import voice.core.audiobookshelf.api.BookmarkRequest
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.itemId
import voice.core.data.Book
import voice.core.data.Bookmark
import voice.core.data.isRemote
import voice.core.data.markForPosition
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.logging.api.Logger
import java.io.IOException
import java.time.Instant

/**
 * Brings bookmarks over in both directions. The server only knows a time and a title, so a bookmark is
 * identified by its book and the second it points to. Bookmarks the sleep timer sets stay on this device.
 */
@SingleIn(AppScope::class)
@Inject
internal class BookmarkSync(
  private val http: AudiobookshelfHttp,
  private val bookRepository: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  @SyncedBookmarksStore
  private val syncedBookmarksStore: DataStore<Map<String, String>>,
) {

  private val mutex = Mutex()

  suspend fun sync(
    account: Account,
    serverBookmarks: List<AbsBookmark>,
    books: List<Book>? = null,
  ) = mutex.withLock {
    val api = http.authenticatedApi(account.serverUrl)
    val remoteBooks = (books ?: bookRepository.all()).filter { it.id.isRemote }
    val serverByItem = serverBookmarks.groupBy { it.libraryItemId }
    val synced = syncedBookmarksStore.data.first().toMutableMap()
    try {
      remoteBooks.forEach { book ->
        val itemId = book.id.itemId ?: return@forEach
        syncBook(api, book, itemId, serverByItem[itemId].orEmpty(), synced)
      }
    } catch (e: IOException) {
      Logger.d("Could not sync the bookmarks: $e")
    } finally {
      syncedBookmarksStore.updateData { synced.toMap() }
    }
  }

  private suspend fun syncBook(
    api: AudiobookshelfApi,
    book: Book,
    itemId: String,
    server: List<AbsBookmark>,
    synced: MutableMap<String, String>,
  ) {
    val local = bookmarkRepo.bookmarks(book.content)
      .filter { !it.setBySleepTimer }
      .associateBy { key(itemId, book.secondOf(it)) }
    val onServer = server.associateBy { key(itemId, it.time.toLong()) }

    onServer.forEach { (key, bookmark) ->
      if (key in local) return@forEach
      if (key in synced) {
        if (api.deleteBookmark(itemId, bookmark.time.asPathSegment()).isSuccessfulOrGone()) synced -= key
      } else {
        bookmarkRepo.addBookmark(book.bookmarkAt(bookmark))
        synced[key] = bookmark.title.orEmpty()
      }
    }

    local.forEach { (key, bookmark) ->
      val serverBookmark = onServer[key]
      val title = bookmark.title?.takeIf { it.isNotBlank() } ?: book.markNameAt(bookmark)
      val syncedTitle = synced[key]
      when {
        serverBookmark != null -> {
          val serverTitle = serverBookmark.title.orEmpty()
          synced[key] = when {
            syncedTitle == null || serverTitle == title -> title
            title == syncedTitle -> {
              bookmarkRepo.addBookmark(bookmark.copy(title = serverTitle))
              serverTitle
            }
            serverTitle == syncedTitle -> {
              // the server finds the bookmark by its exact time, which other apps store with a fraction
              val request = BookmarkRequest(time = serverBookmark.time, title = title)
              if (!api.updateBookmark(itemId, request).isSuccessful) return@forEach
              title
            }
            // both renamed it, and this device is where the listener is right now
            else -> {
              // the server finds the bookmark by its exact time, which other apps store with a fraction
              val request = BookmarkRequest(time = serverBookmark.time, title = title)
              if (!api.updateBookmark(itemId, request).isSuccessful) return@forEach
              title
            }
          }
        }
        syncedTitle != null -> {
          bookmarkRepo.deleteBookmark(bookmark.id)
          synced -= key
        }
        else -> {
          val request = BookmarkRequest(time = book.secondOf(bookmark).toDouble(), title = title)
          if (api.addBookmark(itemId, request).isSuccessful) synced[key] = title
        }
      }
    }
  }

  private fun key(
    itemId: String,
    second: Long,
  ): String = "$itemId@$second"
}

private fun Response<Unit>.isSuccessfulOrGone(): Boolean = isSuccessful || code() == 404

// the server finds a bookmark by its exact time, which other apps store with a fraction
private fun Double.asPathSegment(): String = if (this % 1.0 == 0.0) toLong().toString() else toString()

private fun Book.secondOf(bookmark: Bookmark): Long {
  val chapterStart = chapters.takeWhile { it.id != bookmark.chapterId }.sumOf { it.duration }
  return (chapterStart + bookmark.time) / 1000
}

private fun Book.bookmarkAt(bookmark: AbsBookmark): Bookmark {
  val position = positionAt((bookmark.time * 1000).toLong())
  return Bookmark(
    bookId = id,
    chapterId = position.chapterId,
    title = bookmark.title?.takeIf { it.isNotBlank() },
    time = position.positionInChapter,
    addedAt = if (bookmark.createdAt > 0) Instant.ofEpochMilli(bookmark.createdAt) else Instant.now(),
    setBySleepTimer = false,
    id = Bookmark.Id.random(),
  )
}

/**
 * The server needs a title, and the chapter tells most about where an untitled bookmark is.
 */
private fun Book.markNameAt(bookmark: Bookmark): String {
  val chapter = chapters.firstOrNull { it.id == bookmark.chapterId }
  return chapter?.markForPosition(bookmark.time)?.name?.takeIf { it.isNotBlank() }
    ?: chapter?.name?.takeIf { it.isNotBlank() }
    ?: content.name
}
