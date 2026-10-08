package voice.core.scanner

import android.net.Uri
import android.provider.DocumentsContract
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.Inject
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ListeningHistoryRepo
import voice.core.data.repo.ListeningStatsRepo
import voice.core.data.store.CurrentBookStore

/**
 * Hands progress over when audio files show up under a new book id. This happens when a folder's mode changes
 * (a single book becomes several books, or the other way round) or when a folder is added again through another
 * folder. Chapters are matched by their document, which stays the same across folders.
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
    previousBooks: PreviousBooks,
  ): BookContent {
    val chapterByKey = content.chapters.associateBy { it.documentKey() }
    val previous = previousBooks.sharingChapters(chapterByKey.keys)
    if (previous.isEmpty()) return content

    moveBookmarks(previous, content.id, chapterByKey)
    moveListeningHistory(previous, content.id, chapterByKey)

    val playedHere = previous.filter { it.currentChapter.documentKey() in chapterByKey }
    moveCurrentBook(playedHere, content.id)
    // sessions don't know their chapters, so they go along with where the listener was
    listeningStatsRepo.moveToBook(from = playedHere.map { it.id }, to = content.id)

    val source = playedHere
      .maxByOrNull { it.lastPlayedAt }
      ?.takeIf { it.lastPlayedAt > content.lastPlayedAt }
      ?: return content

    // only the same book keeps what the user set up for it, a split up book gets the names from its files
    val sameBook = source.id.toUri().documentKey() == content.id.toUri().documentKey()
    return content.copy(
      currentChapter = chapterByKey.getValue(source.currentChapter.documentKey()),
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
    chapterByKey: Map<String, ChapterId>,
  ) {
    previous
      .flatMap { bookmarkRepo.bookmarks(it) }
      .distinctBy { it.id }
      .forEach { bookmark ->
        val chapter = chapterByKey[bookmark.chapterId.documentKey()] ?: return@forEach
        val moved = bookmark.copy(bookId = bookId, chapterId = chapter)
        if (moved != bookmark) {
          bookmarkRepo.addBookmark(moved)
        }
      }
  }

  private suspend fun moveListeningHistory(
    previous: List<BookContent>,
    bookId: BookId,
    chapterByKey: Map<String, ChapterId>,
  ) {
    val chapters = previous
      .flatMap { it.chapters }
      .distinct()
      .mapNotNull { chapter -> chapterByKey[chapter.documentKey()]?.let { chapter to it } }
      .toMap()
    listeningHistoryRepo.moveToBook(
      from = previous.map { it.id },
      to = bookId,
      chapters = chapters,
    )
  }
}

/**
 * The books a scan no longer finds, indexed by the documents of their chapters.
 */
internal class PreviousBooks(
  books: List<BookContent>,
  scanned: Set<BookId>,
) {

  private val byChapterKey: Map<String, List<BookContent>> = books
    .filter { it.id !in scanned }
    .flatMap { book -> book.chapters.map { it.documentKey() to book } }
    .groupBy(keySelector = { it.first }, valueTransform = { it.second })

  fun sharingChapters(chapterKeys: Collection<String>): List<BookContent> {
    return chapterKeys
      .flatMap { byChapterKey[it].orEmpty() }
      .distinctBy { it.id }
  }
}

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
