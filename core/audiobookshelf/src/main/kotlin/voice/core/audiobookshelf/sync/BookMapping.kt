package voice.core.audiobookshelf.sync

import voice.core.audiobookshelf.AudiobookshelfIds
import voice.core.audiobookshelf.api.AbsChapter
import voice.core.audiobookshelf.api.AbsLibraryItem
import voice.core.audiobookshelf.api.AbsTrack
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.MarkData
import java.time.Instant
import kotlin.math.roundToLong

/**
 * Chapters that start this close to a track boundary belong to the next track. Durations of the tracks and the
 * chapters don't line up to the millisecond.
 */
private const val BOUNDARY_TOLERANCE_SECONDS = 0.5

/**
 * The tracks of an item as Voice chapters. Audiobookshelf has chapters across the whole book, so each track gets
 * the chapters that fall into it as its marks. The item's update time stands in for the file's modification time,
 * which tells the sync whether the item changed since it was stored.
 */
internal fun AbsLibraryItem.chapters(): List<Chapter> {
  val bookChapters = media.chapters.orEmpty().sortedBy { it.start }
  return media.tracks.orEmpty()
    .sortedBy { it.index }
    .map { track ->
      val marks = track.marks(bookChapters)
      Chapter(
        id = AudiobookshelfIds.chapterId(itemId = id, trackIndex = track.index, ino = track.ino),
        name = marks.singleOrNull()?.name?.takeIf { it.isNotBlank() } ?: track.name(),
        duration = (track.duration * 1000).roundToLong().coerceAtLeast(1),
        fileLastModified = Instant.ofEpochMilli(updatedAt),
        fileSize = track.metadata?.size ?: 0L,
        markData = marks,
      )
    }
}

private fun AbsTrack.marks(bookChapters: List<AbsChapter>): List<MarkData> {
  val trackStart = startOffset
  val trackEnd = startOffset + duration
  val running = bookChapters.lastOrNull { it.start <= trackStart + BOUNDARY_TOLERANCE_SECONDS }
  val startingInside = bookChapters.filter {
    it.start > trackStart + BOUNDARY_TOLERANCE_SECONDS && it.start < trackEnd - BOUNDARY_TOLERANCE_SECONDS
  }
  return buildList {
    if (running != null) {
      add(MarkData(startMs = 0, name = running.title.orEmpty()))
    }
    startingInside.forEach { chapter ->
      add(MarkData(startMs = ((chapter.start - trackStart) * 1000).roundToLong(), name = chapter.title.orEmpty()))
    }
  }
}

private fun AbsTrack.name(): String {
  val fileName = metadata?.filename ?: title ?: return "Track $index"
  return fileName.substringBeforeLast('.').ifEmpty { fileName }
}

/**
 * The item as a book, keeping what the listener did with it on this device: where they are, how fast they
 * listen, and when they last played it.
 */
internal fun AbsLibraryItem.toBookContent(
  chapters: List<Chapter>,
  existing: BookContent?,
  previousChapters: List<Chapter> = emptyList(),
): BookContent {
  val chapterIds = chapters.map { it.id }
  val metadata = media.metadata
  val series = metadata.series?.firstOrNull()
  val position = existing?.let { content ->
    if (content.currentChapter in chapterIds) {
      BookPosition(content.currentChapter, content.positionInChapter)
    } else {
      // the server replaced the files, the place in the book stays
      previousChapters.positionInBook(content.currentChapter, content.positionInChapter)?.let(chapters::positionAt)
    }
  } ?: BookPosition(chapterIds.first(), 0L)
  return BookContent(
    id = AudiobookshelfIds.bookId(id),
    playbackSpeed = existing?.playbackSpeed ?: 1F,
    skipSilence = existing?.skipSilence ?: false,
    isActive = true,
    lastPlayedAt = existing?.lastPlayedAt ?: Instant.EPOCH,
    author = metadata.authorName?.takeIf { it.isNotBlank() },
    name = metadata.title?.takeIf { it.isNotBlank() } ?: chapters.first().name ?: id,
    addedAt = existing?.addedAt ?: Instant.ofEpochMilli(addedAt),
    chapters = chapterIds,
    currentChapter = position.chapterId,
    positionInChapter = position.positionInChapter.coerceIn(0, chapters.first { it.id == position.chapterId }.duration),
    cover = existing?.cover,
    gain = existing?.gain ?: 0F,
    genre = metadata.genres.joinToString(", ").takeIf { it.isNotBlank() },
    narrator = metadata.narratorName?.takeIf { it.isNotBlank() },
    series = series?.name?.takeIf { it.isNotBlank() } ?: metadata.seriesName?.takeIf { it.isNotBlank() },
    part = series?.sequence?.takeIf { it.isNotBlank() },
  )
}

internal data class BookPosition(
  val chapterId: ChapterId,
  val positionInChapter: Long,
)

/**
 * Where [positionMs], counted from the start of the book, lies within its chapters.
 */
internal fun Book.positionAt(positionMs: Long): BookPosition = chapters.positionAt(positionMs)

internal fun List<Chapter>.positionAt(positionMs: Long): BookPosition {
  var remaining = positionMs.coerceIn(0, sumOf { it.duration })
  forEachIndexed { index, chapter ->
    if (remaining < chapter.duration || index == lastIndex) {
      return BookPosition(chapter.id, remaining.coerceAtMost(chapter.duration))
    }
    remaining -= chapter.duration
  }
  error("A book always has chapters")
}

/**
 * How far into the book [positionInChapter] of [chapterId] is, or null when the chapter isn't one of these.
 */
internal fun List<Chapter>.positionInBook(
  chapterId: ChapterId,
  positionInChapter: Long,
): Long? {
  if (none { it.id == chapterId }) return null
  return takeWhile { it.id != chapterId }.sumOf { it.duration } + positionInChapter
}

/**
 * The server marks a book as finished once less than ten seconds are left. Voice uses the same line for the
 * sync, as the server resets a finished book to its start when it's told the book isn't finished.
 */
internal const val FINISHED_REMAINING_MS = 10_000L

internal val Book.isFinished: Boolean get() = position > 0 && duration - position < FINISHED_REMAINING_MS
