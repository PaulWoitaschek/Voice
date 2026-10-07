package voice.core.playback.session

import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ChapterMark
import voice.core.data.durationMs
import voice.core.data.markForPosition

internal data class PlaybackItem(
  val index: Int,
  val bookId: BookId,
  val chapter: Chapter,
  val markIndex: Int,
  val mark: ChapterMark,
) {
  val mediaId: MediaId.ChapterMark
    get() = MediaId.ChapterMark(
      bookId = bookId,
      chapterId = chapter.id,
      markIndex = markIndex,
      startMs = mark.startMs,
      endMs = mark.endMs,
    )
}

internal fun Book.playbackItems(): List<PlaybackItem> {
  var index = 0
  return chapters.flatMap { chapter ->
    chapter.chapterMarks.mapIndexed { markIndex, mark ->
      PlaybackItem(
        index = index++,
        bookId = id,
        chapter = chapter,
        markIndex = markIndex,
        mark = mark,
      )
    }
  }
}

/**
 * The playlist as the session and the UI see it: one entry per chapter mark, together with the
 * index of the audio file each entry lives in. The underlying player only holds one item per file.
 */
internal class ChapterMarkPlaylist(
  val items: List<PlaybackItem>,
  private val fileIndexByItem: IntArray,
) {

  fun fileIndexOf(itemIndex: Int): Int = fileIndexByItem[itemIndex]

  fun itemIndexFor(
    fileIndex: Int,
    positionInFileMs: Long,
  ): Int? {
    var result: Int? = null
    for (index in items.indices) {
      if (fileIndexByItem[index] != fileIndex) continue
      if (result == null || items[index].mark.startMs <= positionInFileMs) {
        result = index
      }
      if (items[index].mark.startMs > positionInFileMs) break
    }
    return result
  }
}

internal fun Book.chapterMarkPlaylist(): ChapterMarkPlaylist {
  val items = playbackItems()
  val fileIndexByChapterId = chapters.withIndex().associate { (index, chapter) -> chapter.id to index }
  return ChapterMarkPlaylist(
    items = items,
    fileIndexByItem = IntArray(items.size) { fileIndexByChapterId.getValue(items[it].chapter.id) },
  )
}

internal fun Book.playbackItemForPosition(
  chapterId: ChapterId,
  positionInChapterMs: Long,
): PlaybackItem? {
  val chapter = chapters.firstOrNull { it.id == chapterId } ?: return null
  val mark = chapter.markForPosition(positionInChapterMs)
  return playbackItems().firstOrNull {
    it.chapter.id == chapterId && it.mark == mark
  }
}

internal val MediaId.bookId: BookId?
  get() = when (this) {
    is MediaId.Book -> id
    is MediaId.Chapter -> bookId
    is MediaId.ChapterMark -> bookId
    MediaId.Recent,
    MediaId.Root,
    -> null
  }

internal val MediaId.realChapterId: ChapterId?
  get() = when (this) {
    is MediaId.Chapter -> chapterId
    is MediaId.ChapterMark -> chapterId
    is MediaId.Book,
    MediaId.Recent,
    MediaId.Root,
    -> null
  }

internal fun MediaId.positionInChapter(positionInCurrentMediaItemMs: Long): Long? {
  return when (this) {
    is MediaId.Chapter -> positionInCurrentMediaItemMs
    is MediaId.ChapterMark -> startMs + positionInCurrentMediaItemMs
    is MediaId.Book,
    MediaId.Recent,
    MediaId.Root,
    -> null
  }
}

internal fun PlaybackItem.positionInMediaItem(positionInChapterMs: Long): Long {
  return (positionInChapterMs - mark.startMs).coerceIn(0L, mark.durationMs)
}
