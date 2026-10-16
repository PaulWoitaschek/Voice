package voice.core.foldersync

import android.net.Uri
import android.provider.DocumentsContract
import voice.core.data.Book
import voice.core.data.ChapterId
import voice.core.data.syncKey
import java.time.Instant
import kotlin.math.abs

internal data class KeyedBook(
  val key: String,
  val book: Book,
)

/**
 * The books from [folder] that another device can recognize.
 */
internal fun List<Book>.keyedIn(folder: Uri): List<KeyedBook> {
  val tree = folder.tree() ?: return emptyList()
  return mapNotNull { book ->
    if (book.id.toUri().tree() != tree) return@mapNotNull null
    book.chapters.syncKey()?.let { KeyedBook(it, book) }
  }
}

// a single file added on its own has a document uri, not a tree uri
private fun Uri.tree(): Pair<String?, String>? = if (DocumentsContract.isTreeUri(this)) {
  authority to DocumentsContract.getTreeDocumentId(this)
} else {
  null
}

internal data class TakenOverPosition(
  val chapterId: ChapterId,
  val positionInChapter: Long,
  val playedAt: Instant,
)

/**
 * Where [remote] moves this book to, or null if it was played here since or is at the same place.
 */
internal fun Book.positionFrom(remote: SyncedPlace): TakenOverPosition? {
  if (remote.playedAt <= content.lastPlayedAt.toEpochMilli()) return null
  val chapter = chapters.getOrNull(remote.file) ?: return null
  val position = remote.position.coerceIn(0, chapter.duration)
  // taking over a place seeks, which makes it newer here, so a small difference would go back and forth
  val samePlace = chapter.id == content.currentChapter && abs(position - content.positionInChapter) <= SAME_PLACE_MS
  if (samePlace) return null
  return TakenOverPosition(
    chapterId = chapter.id,
    positionInChapter = position,
    playedAt = Instant.ofEpochMilli(remote.playedAt),
  )
}

private const val SAME_PLACE_MS = 1500
