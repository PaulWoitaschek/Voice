package voice.core.audiobookshelf.sync

import dev.zacsweers.metro.Inject
import java.io.IOException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import voice.core.audiobookshelf.AudiobookshelfIds
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.AbsLibraryItem
import voice.core.audiobookshelf.api.AudiobookshelfApi
import voice.core.audiobookshelf.api.BatchGetRequest
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Chapter
import voice.core.data.CustomMetadata
import voice.core.data.isRemote
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ChapterRepo
import voice.core.logging.api.Logger

private const val PAGE_SIZE = 200
private const val DETAILS_CHUNK_SIZE = 50
private const val BOOKS_PER_WRITE = 1_000

@Inject
internal class LibrarySync(
  private val http: AudiobookshelfHttp,
  private val contentRepo: BookContentRepo,
  private val bookRepository: BookRepository,
  private val chapterRepo: ChapterRepo,
  private val bookmarkRepo: BookmarkRepo,
  private val accountStore: AccountStore,
) {

  suspend fun sync(account: Account) {
    val api = http.authenticatedApi(account.serverUrl)
    val books =
      account.libraryIds
        .flatMap { libraryId -> allItems(api, libraryId) }
        .filter { it.mediaType == "book" }
        .distinctBy { it.id }
    val items = books.filter { !it.isMissing && !it.isInvalid && it.media.numTracks > 0 }
    // the server flags items whose files it can't reach right now, their books stay as they are,
    // downloads included
    val flagged =
      books.filter { it.isMissing || it.isInvalid }.map { AudiobookshelfIds.bookId(it.id) }.toSet()

    val stored = contentRepo.all().filter { it.id.isRemote }.associateBy { it.id }

    val changed = items.filter { item ->
      val content = stored[AudiobookshelfIds.bookId(item.id)] ?: return@filter true
      val firstChapter = chapterRepo.get(content.chapters.first())
      firstChapter == null || firstChapter.fileLastModified.toEpochMilliseconds() != item.updatedAt
    }
    Logger.d("Audiobookshelf has ${items.size} books, ${changed.size} of them changed")

    // each write updates the whole library, so a large one shows in a few steps
    changed.chunked(BOOKS_PER_WRITE).forEach { books ->
      val fetched = mutableListOf<AbsLibraryItem>()
      try {
        books.chunked(DETAILS_CHUNK_SIZE).forEach { chunk ->
          fetched +=
            api.items(BatchGetRequest(chunk.map { it.id })).libraryItems.filter {
              !it.media.tracks.isNullOrEmpty()
            }
        }
      } catch (e: IOException) {
        // on a weak connection, the next sync continues from the books that got through
        store(fetched)
        throw e
      } catch (e: HttpException) {
        store(fetched)
        throw e
      }
      store(fetched)
    }

    val onServer = items.map { AudiobookshelfIds.bookId(it.id) }.toSet()
    val libraryOf = books.associate { AudiobookshelfIds.bookId(it.id) to it.libraryId }
    val remote = contentRepo.all().filter { it.id.isRemote }
    // a backup leaves out the cover files of older versions, the cover then loads from the server
    val missingCovers = remote.filter { it.metadata.cover?.exists() == false }.map { it.id }.toSet()
    fun BookContent.synced(): BookContent {
      val active = if (id in flagged) isActive else id in onServer
      // unchanged books aren't stored again, so they get their library here
      return copy(
        isActive = active,
        serverLibraryId = libraryOf[id] ?: serverLibraryId,
        metadata = if (id in missingCovers) metadata.copy(cover = null) else metadata,
      )
    }
    val outdated = remote.filter { it.synced() != it }.map { it.id }
    bookRepository.updateBooks(outdated.toSet()) { it.synced() }
    // stored after the books got their library, so a library never shows without its books
    val libraryNames = api.libraries().libraries.associate { it.id to it.name }
    accountStore.updateData { it?.copy(libraryNames = libraryNames) }
  }

  private suspend fun allItems(
    api: AudiobookshelfApi,
    libraryId: String,
  ): List<AbsLibraryItem> {
    val items = mutableListOf<AbsLibraryItem>()
    var page = 0
    while (true) {
      val response =
        try {
          api.libraryItems(libraryId = libraryId, limit = PAGE_SIZE, page = page)
        } catch (e: HttpException) {
          // a library that was deleted or that the user can't see anymore has no books for Voice
          if (e.code() == 404 || e.code() == 403) {
            Logger.d("A selected library is gone: HTTP ${e.code()}")
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

  private suspend fun store(items: List<AbsLibraryItem>) {
    val books = items.mapNotNull { item ->
      val chapters =
        skipBroken { item.chapters() }?.takeIf { it.isNotEmpty() } ?: return@mapNotNull null
      val existing = contentRepo.get(AudiobookshelfIds.bookId(item.id))
      // read before the new chapters replace them under the same ids
      val previousChapters = existing?.chapters?.mapNotNull { chapterRepo.get(it) }.orEmpty()
      FetchedBook(item, chapters, existing, previousChapters)
    }
    chapterRepo.put(books.flatMap { it.chapters })
    val (known, new) = books.partition { it.existing != null }
    withContext(NonCancellable) {
      contentRepo.put(new.mapNotNull { it.content(current = null) })
      // a pause or a seek takes the same lock, so each book is read again inside it
      val knownById = known.associateBy { it.id }
      bookRepository.updateBooks(knownById.keys) { current ->
        knownById.getValue(current.id).content(current) ?: current
      }
      known.forEach { book ->
        book.existing!!.metadata.cover?.delete()
        // the books point at the new files right away, their bookmarks have to follow
        moveBookmarks(book.existing, book.previousChapters, book.chapters)
      }
    }
  }

  // bookmarks point into a file, and the server can replace the files of a book while the places in
  // it stay
  private suspend fun moveBookmarks(
    existing: BookContent,
    previousChapters: List<Chapter>,
    chapters: List<Chapter>,
  ) {
    val chapterIds = chapters.map { it.id }.toSet()
    bookmarkRepo
      .bookmarks(existing)
      .filter { it.chapterId !in chapterIds }
      .forEach { bookmark ->
        val positionInBook =
          previousChapters.positionInBook(bookmark.chapterId, bookmark.time) ?: return@forEach
        val position = chapters.positionAt(positionInBook)
        bookmarkRepo.addBookmark(
          bookmark.copy(chapterId = position.chapterId, time = position.positionInChapter),
        )
      }
  }
}

private class FetchedBook(
  val item: AbsLibraryItem,
  val chapters: List<Chapter>,
  val existing: BookContent?,
  val previousChapters: List<Chapter>,
) {

  val id = AudiobookshelfIds.bookId(item.id)

  fun content(current: BookContent?): BookContent? {
    return skipBroken {
      val content =
        item.toBookContent(chapters, current, previousChapters).let {
          // a change on the server can bring a new cover, older versions kept the old one in a file
          it.copy(metadata = it.metadata.copy(cover = null))
        }
      // the init block checks that the chapters and the position fit together
      @Suppress("RETURN_VALUE_NOT_USED") Book(content, chapters, CustomMetadata(content.id))
      content
    }
  }
}

private inline fun <T> skipBroken(block: () -> T): T? =
  try {
    block()
  } catch (e: IllegalStateException) {
    Logger.w(e, "Skipping a server book")
    null
  } catch (e: IllegalArgumentException) {
    Logger.w(e, "Skipping a server book")
    null
  }
