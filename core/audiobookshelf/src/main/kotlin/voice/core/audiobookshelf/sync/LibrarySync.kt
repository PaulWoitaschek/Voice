package voice.core.audiobookshelf.sync

import android.content.Context
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import okhttp3.Request
import retrofit2.HttpException
import voice.core.audiobookshelf.AudiobookshelfIds
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.api.AbsLibraryItem
import voice.core.audiobookshelf.api.AudiobookshelfApi
import voice.core.audiobookshelf.api.BatchGetRequest
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.itemId
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Chapter
import voice.core.data.isRemote
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ChapterRepo
import voice.core.logging.api.Logger
import java.io.File
import java.io.IOException

private const val PAGE_SIZE = 200
private const val DETAILS_CHUNK_SIZE = 50

/**
 * Mirrors the books of the chosen libraries into Voice's library. Only items that changed since the last sync are
 * fetched with their tracks and chapters, so a sync of an unchanged library costs a few small requests.
 */
@Inject
internal class LibrarySync(
  private val http: AudiobookshelfHttp,
  private val contentRepo: BookContentRepo,
  private val chapterRepo: ChapterRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val context: Context,
) {

  suspend fun sync(account: Account) {
    val api = http.authenticatedApi(account.serverUrl)
    val books = account.libraryIds
      .flatMap { libraryId -> allItems(api, libraryId) }
      .filter { it.mediaType == "book" }
      .distinctBy { it.id }
    val items = books.filter { !it.isMissing && !it.isInvalid && it.media.numTracks > 0 }
    // the server flags items whose files it can't reach right now, their books stay as they are, downloads included
    val flagged = books.filter { it.isMissing || it.isInvalid }.map { AudiobookshelfIds.bookId(it.id) }.toSet()

    val stored = contentRepo.all()
      .filter { it.id.isRemote }
      .associateBy { it.id }

    val changed = items.filter { item ->
      val content = stored[AudiobookshelfIds.bookId(item.id)] ?: return@filter true
      val firstChapter = chapterRepo.get(content.chapters.first())
      firstChapter == null || firstChapter.fileLastModified.toEpochMilli() != item.updatedAt
    }
    Logger.d("Audiobookshelf has ${items.size} books, ${changed.size} of them changed")

    changed.chunked(DETAILS_CHUNK_SIZE).forEach { chunk ->
      api.items(BatchGetRequest(chunk.map { it.id }))
        .libraryItems
        .filter { !it.media.tracks.isNullOrEmpty() }
        .forEach { item ->
          try {
            store(item)
          } catch (e: CancellationException) {
            throw e
          } catch (e: IllegalStateException) {
            Logger.w(e, "Skipping ${item.id}")
          } catch (e: IllegalArgumentException) {
            Logger.w(e, "Skipping ${item.id}")
          }
        }
    }

    val onServer = items.map { AudiobookshelfIds.bookId(it.id) }.toSet()
    contentRepo.all()
      .filter { it.id.isRemote }
      .forEach { content ->
        if (content.id in flagged) return@forEach
        val active = content.id in onServer
        if (content.isActive != active) {
          contentRepo.put(content.copy(isActive = active))
        }
      }

    downloadMissingCovers(account, items)
  }

  private suspend fun allItems(
    api: AudiobookshelfApi,
    libraryId: String,
  ): List<AbsLibraryItem> {
    val items = mutableListOf<AbsLibraryItem>()
    var page = 0
    while (true) {
      val response = try {
        api.libraryItems(libraryId = libraryId, limit = PAGE_SIZE, page = page)
      } catch (e: HttpException) {
        // a library that was deleted or that the user can't see anymore has no books for Voice
        if (e.code() == 404 || e.code() == 403) {
          Logger.w(e, "The library $libraryId is gone")
          return emptyList()
        }
        throw e
      }
      items += response.results
      if (response.results.size < PAGE_SIZE || items.size >= response.total) break
      page++
    }
    return items
  }

  private suspend fun store(item: AbsLibraryItem) {
    val chapters = item.chapters()
    if (chapters.isEmpty()) return
    // read right before writing, so a pause or a seek during the requests isn't undone
    val existing = contentRepo.get(AudiobookshelfIds.bookId(item.id))
    val previousChapters = existing?.chapters?.mapNotNull { chapterRepo.get(it) }.orEmpty()
    chapters.forEach { chapterRepo.put(it) }
    val content = item.toBookContent(chapters, existing, previousChapters)
    // an update of the item can bring a new cover, so the next pass downloads it again
    val cover = existing?.cover?.takeIf { it.name == coverFileName(item) }
    if (cover == null) existing?.cover?.delete()
    val updated = content.copy(cover = cover)
    // the init block checks that the chapters and the position fit together
    @Suppress("RETURN_VALUE_NOT_USED")
    Book(updated, chapters)
    // the book points at the new files right away, its bookmarks have to follow
    withContext(NonCancellable) {
      contentRepo.put(updated)
      if (existing != null) moveBookmarks(existing, previousChapters, chapters)
    }
  }

  // bookmarks point into a file, and the server can replace the files of a book while the places in it stay
  private suspend fun moveBookmarks(
    existing: BookContent,
    previousChapters: List<Chapter>,
    chapters: List<Chapter>,
  ) {
    val chapterIds = chapters.map { it.id }.toSet()
    bookmarkRepo.bookmarks(existing)
      .filter { it.chapterId !in chapterIds }
      .forEach { bookmark ->
        val positionInBook = previousChapters.positionInBook(bookmark.chapterId, bookmark.time) ?: return@forEach
        val position = chapters.positionAt(positionInBook)
        bookmarkRepo.addBookmark(bookmark.copy(chapterId = position.chapterId, time = position.positionInChapter))
      }
  }

  private suspend fun downloadMissingCovers(
    account: Account,
    items: List<AbsLibraryItem>,
  ) {
    val itemsById = items.associateBy { it.id }
    contentRepo.all()
      .filter { it.id.isRemote && it.isActive && (it.cover == null || !it.cover!!.exists()) }
      .forEach { content ->
        val item = content.id.itemId?.let(itemsById::get) ?: return@forEach
        if (item.media.coverPath == null) return@forEach
        val file = downloadCover(account, item) ?: return@forEach
        val current = contentRepo.get(content.id) ?: return@forEach
        contentRepo.put(current.copy(cover = file))
      }
  }

  private suspend fun downloadCover(
    account: Account,
    item: AbsLibraryItem,
  ): File? = withContext(Dispatchers.IO) {
    val folder = File(context.filesDir, "bookCovers").apply { mkdirs() }
    val file = File(folder, coverFileName(item))
    val urls = listOf(
      "${account.serverUrl}api/items/${item.id}/cover?width=800&format=jpeg",
      "${account.serverUrl}api/items/${item.id}/cover?raw=1",
    )
    for (url in urls) {
      try {
        http.authenticatedClient.newCall(Request.Builder().url(url).build()).execute().use { response ->
          val body = response.body
          if (response.isSuccessful && response.header("Content-Type")?.startsWith("image/") == true) {
            file.outputStream().use { body.byteStream().copyTo(it) }
            return@withContext file
          }
        }
      } catch (e: IOException) {
        Logger.w(e, "Could not download the cover of ${item.id}")
        return@withContext null
      }
    }
    null
  }

  private fun coverFileName(item: AbsLibraryItem): String = "abs-${item.id}-${item.updatedAt}.jpg"
}
