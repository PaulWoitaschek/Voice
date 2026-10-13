package voice.core.scanner

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ChapterRepo
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.store.CurrentBookStore
import java.time.Instant

/**
 * Hands progress over when audio files show up under a new book id. This happens when a folder's mode changes
 * (a single book becomes several books, or the other way round), when a folder is added again through another
 * folder, or when a book is renamed or moved. Chapters are matched by their document, which stays the same across
 * folders, and otherwise by their file.
 */
@Inject
internal class ProgressCarryOver(
  private val bookmarkRepo: BookmarkRepo,
  private val listeningHistoryRepo: ListeningHistoryRepo,
  private val listeningStatsRepo: ListeningStatsRepo,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
) {

  suspend fun carryOver(
    content: BookContent,
    chapters: List<Chapter>,
    previousBooks: PreviousBooks,
  ): BookContent {
    val predecessors = previousBooks.of(content, chapters) ?: return content
    val previous = predecessors.books
    val moved = predecessors.chapters

    moveBookmarks(previous, content.id, moved)
    listeningHistoryRepo.moveToBook(from = previous.map { it.id }, to = content.id, chapters = moved)

    val playedHere = previous.filter { it.currentChapter in moved }
    moveCurrentBook(playedHere, content.id)
    // sessions don't know their chapters, so they go along with where the listener was
    listeningStatsRepo.moveToBook(from = playedHere.map { it.id }, to = content.id)

    val source = playedHere
      .maxByOrNull { it.lastPlayedAt }
      ?.takeIf { it.lastPlayedAt > content.lastPlayedAt }
      ?: return content

    // only the same book keeps what the user set up for it, a split up book gets the names from its files
    val sameBook = predecessors.moved || source.id.toUri().documentKey() == content.id.toUri().documentKey()
    return content.copy(
      currentChapter = moved.getValue(source.currentChapter),
      positionInChapter = source.positionInChapter,
      lastPlayedAt = source.lastPlayedAt,
      playbackSpeed = source.playbackSpeed,
      skipSilence = source.skipSilence,
      gain = source.gain,
      name = if (sameBook) source.name else content.name,
      cover = if (sameBook) source.cover ?: content.cover else content.cover,
      addedAt = if (sameBook) source.addedAt else content.addedAt,
    )
  }

  // the book the user is on is gone, so they go on with the book that now holds its current chapter
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

/**
 * The books a scan no longer finds, indexed by the documents and the files of their chapters.
 */
internal class PreviousBooks(
  books: List<BookContent>,
  scanned: Set<BookId>,
  private val chapterRepo: ChapterRepo,
) {

  private val books = books.filter { it.id !in scanned }

  private val byDocument: Map<String, List<BookContent>> = this.books
    .flatMap { book -> book.chapters.map { it.documentKey() to book } }
    .groupBy(keySelector = { it.first }, valueTransform = { it.second })

  private var byFile: Map<FileFingerprint, List<Pair<BookContent, ChapterId>>>? = null

  /**
   * The books the [content] with its [chapters] takes over from.
   */
  suspend fun of(
    content: BookContent,
    chapters: List<Chapter>,
  ): Predecessors? {
    return sharingDocuments(content.chapters)
      // a book that was played isn't a moved one, even when a removed copy had the same files
      ?: if (content.lastPlayedAt == Instant.EPOCH) sharingFiles(chapters) else null
  }

  private fun sharingDocuments(chapters: List<ChapterId>): Predecessors? {
    val chapterByKey = chapters.associateBy { it.documentKey() }
    val books = chapterByKey.keys
      .flatMap { byDocument[it].orEmpty() }
      .distinctBy { it.id }
    if (books.isEmpty()) return null
    val moved = books
      .flatMap { it.chapters }
      .mapNotNull { chapter -> chapterByKey[chapter.documentKey()]?.let { chapter to it } }
      .toMap()
    return Predecessors(books = books, chapters = moved, moved = false)
  }

  private suspend fun sharingFiles(chapters: List<Chapter>): Predecessors? {
    val byFile = byFile()
    val movedByBook = chapters.byFingerprint()
      .flatMap { (fingerprint, chapter) ->
        byFile[fingerprint].orEmpty().map { (book, previous) -> book to (previous to chapter) }
      }
      .groupBy(keySelector = { it.first }, valueTransform = { it.second })
      // a shared file, like an intro, doesn't make it the same book
      .filter { (book, moved) -> moved.size * 2 > book.chapters.size && moved.size * 2 > chapters.size }
    if (movedByBook.isEmpty()) return null
    return Predecessors(
      books = movedByBook.keys.toList(),
      chapters = movedByBook.values.flatten().toMap(),
      moved = true,
    )
  }

  private suspend fun byFile(): Map<FileFingerprint, List<Pair<BookContent, ChapterId>>> {
    return byFile ?: books
      .flatMap { book ->
        book.chapters
          .mapNotNull { chapterRepo.get(it) }
          .byFingerprint()
          .map { (fingerprint, chapter) -> fingerprint to (book to chapter) }
      }
      .groupBy(keySelector = { it.first }, valueTransform = { it.second })
      .also { byFile = it }
  }
}

/**
 * The books a book takes over from, and where each of their chapters is in it.
 */
internal class Predecessors(
  val books: List<BookContent>,
  val chapters: Map<ChapterId, ChapterId>,
  /** Whether they were found by their files, which makes them the same book in another place. */
  val moved: Boolean,
)

private fun ChapterId.documentKey(): String = value.toUri().documentKey()

/**
 * Identifies a document independent of the folder it was opened through.
 */
internal fun Uri.documentKey(): String {
  if (scheme != "content") return toString()
  return try {
    "$authority:${DocumentsContract.getDocumentId(this)}"
  } catch (_: IllegalArgumentException) {
    toString()
  }
}
