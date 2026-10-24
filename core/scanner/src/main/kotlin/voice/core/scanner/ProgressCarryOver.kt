package voice.core.scanner

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.FileFingerprint
import voice.core.data.fingerprints
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ChapterRepo
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.store.CurrentBookStore

// Audio files show up under a new book id when a folder's mode changes, when a folder is added again through another
// folder, or when a book is renamed or moved. Documents stay the same across folders, files across moves.
@Inject
internal class ProgressCarryOver(
  private val bookmarkRepo: BookmarkRepo,
  private val listeningHistoryRepo: ListeningHistoryRepo,
  private val listeningStatsRepo: ListeningStatsRepo,
  private val currentBookStore: CurrentBookStore,
) {

  suspend fun carryOver(
    content: BookContent,
    previousBooks: PreviousBooks,
  ): BookContent {
    val predecessors = previousBooks.of(content) ?: return content
    val previous = predecessors.books
    val moved = predecessors.chapters

    moveBookmarks(previous, content.id, moved)
    listeningHistoryRepo.moveToBook(from = previous.map { it.id }, to = content.id, chapters = moved)

    val playedHere = previous.filter { it.currentChapter in moved }
    moveCurrentBook(playedHere, content.id)
    // sessions don't know their chapters, so they go along with where the listener was
    listeningStatsRepo.moveToBook(from = playedHere.map { it.id }, to = content.id)

    val withEdits = predecessors.sameBook?.let { content.withEditsOf(it) } ?: content
    val source = playedHere
      .maxByOrNull { it.lastPlayedAt }
      ?.takeIf { it.lastPlayedAt > content.lastPlayedAt }
      ?: return withEdits
    return withEdits.copy(
      currentChapter = moved.getValue(source.currentChapter),
      positionInChapter = source.positionInChapter,
      lastPlayedAt = source.lastPlayedAt,
      playbackSpeed = source.playbackSpeed,
      skipSilence = source.skipSilence,
      gain = source.gain,
    )
  }

  private fun BookContent.withEditsOf(sameBook: BookContent): BookContent {
    return copy(
      addedAt = sameBook.addedAt,
      customCover = sameBook.customCover,
      // the files are the same, so their cover is as well
      metadata = metadata.copy(cover = metadata.cover ?: sameBook.metadata.cover),
    ).withCustomName(sameBook.customName)
  }

  private suspend fun moveCurrentBook(
    playedHere: List<BookContent>,
    bookId: BookId,
  ) {
    if (playedHere.isEmpty()) return
    val movedIds = playedHere.map { it.id }.toSet()
    currentBookStore.updateData { current ->
      if (current in movedIds) bookId else current
    }
  }

  private suspend fun moveBookmarks(
    previous: List<BookContent>,
    bookId: BookId,
    chapters: Map<ChapterId, ChapterId>,
  ) {
    previous
      .flatMap { bookmarkRepo.bookmarks(it) }
      .distinctBy { it.id }
      .forEach { bookmark ->
        val chapter = chapters[bookmark.chapterId] ?: return@forEach
        val moved = bookmark.copy(bookId = bookId, chapterId = chapter)
        if (moved != bookmark) {
          bookmarkRepo.addBookmark(moved)
        }
      }
  }
}

internal class PreviousBooks(
  library: List<BookContent>,
  scanned: Set<BookId>,
  private val chapterRepo: ChapterRepo,
) {

  private val books = library.filter { it.id !in scanned }
  private val inLibrary = library.filter { it.isActive }.map { it.id }.toSet()

  private val byDocument: Map<String, List<BookContent>> = this.books
    .flatMap { book -> book.chapters.map { it.documentKey() to book } }
    .groupBy(keySelector = { it.first }, valueTransform = { it.second })

  private var byFiles: Map<List<FileFingerprint>, BookContent>? = null

  suspend fun of(content: BookContent): Predecessors? {
    val isNew = content.id !in inLibrary
    return sharingDocuments(content, isNew)
      // a book that was in the library already is a copy, not the one that moved
      ?: if (isNew) withSameFiles(content.chapters) else null
  }

  private fun sharingDocuments(
    content: BookContent,
    isNew: Boolean,
  ): Predecessors? {
    val chapterByKey = content.chapters.associateBy { it.documentKey() }
    val books = chapterByKey.keys
      .flatMap { byDocument[it].orEmpty() }
      .distinctBy { it.id }
    if (books.isEmpty()) return null
    val moved = books
      .flatMap { it.chapters }
      .mapNotNull { chapter -> chapterByKey[chapter.documentKey()]?.let { chapter to it } }
      .toMap()
    // a split up book gets its names from its files, and a book that was in the library already keeps its own
    val bookKey = content.id.toUri().documentKey()
    val sameBook = if (isNew) {
      books.filter { it.id.toUri().documentKey() == bookKey }.maxByOrNull { it.lastPlayedAt }
    } else {
      null
    }
    return Predecessors(books = books, chapters = moved, sameBook = sameBook)
  }

  private suspend fun withSameFiles(chapters: List<ChapterId>): Predecessors? {
    val stored = chapterRepo.get(chapters)
    if (stored.size != chapters.size) return null
    val files = stored.fingerprints() ?: return null
    val book = byFiles()[files] ?: return null
    return Predecessors(books = listOf(book), chapters = book.chapters.zip(chapters).toMap(), sameBook = book)
  }

  private suspend fun byFiles(): Map<List<FileFingerprint>, BookContent> {
    byFiles?.let { return it }
    val chapters = chapterRepo.get(books.flatMap { it.chapters }).associateBy { it.id }
    return books
      // of copies with the same files, the one played last wins
      .sortedBy { it.lastPlayedAt }
      .mapNotNull { book ->
        val files = book.chapters.map { chapters[it] ?: return@mapNotNull null }.fingerprints()
        files?.let { it to book }
      }
      .toMap()
      .also { byFiles = it }
  }
}

internal class Predecessors(
  val books: List<BookContent>,
  val chapters: Map<ChapterId, ChapterId>,
  val sameBook: BookContent?,
)

private fun ChapterId.documentKey(): String = value.toUri().documentKey()

// the same document opened through another folder has another uri
internal fun Uri.documentKey(): String {
  if (scheme != "content") return toString()
  return try {
    "$authority:${DocumentsContract.getDocumentId(this)}"
  } catch (_: IllegalArgumentException) {
    toString()
  }
}
