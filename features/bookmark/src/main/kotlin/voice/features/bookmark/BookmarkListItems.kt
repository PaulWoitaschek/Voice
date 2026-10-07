package voice.features.bookmark

import voice.core.data.Bookmark
import voice.core.ui.BookBarPin
import voice.core.ui.formatTime
import java.time.Instant
import java.time.ZoneId

/**
 * The bookmarks of a book, either in story order, grouped by chapter around a "You are here" row,
 * or flat with the most recently saved first.
 */
internal fun bookmarkListItems(
  index: BookIndex,
  bookmarks: List<Bookmark>,
  sort: BookmarkSort,
  now: Instant,
  zone: ZoneId,
): List<BookmarkListItem> {
  val located = bookmarks.mapNotNull { bookmark ->
    index.locate(bookmark.chapterId, bookmark.time)?.let { bookmark to it }
  }
  return when (sort) {
    BookmarkSort.Story -> storyOrder(index, located, now, zone)
    BookmarkSort.Recent -> {
      val sorted = located.sortedByDescending { (bookmark, _) -> bookmark.addedAt }
      sorted.mapIndexed { position, (bookmark, location) ->
        BookmarkListItem.Row(
          bookmark = bookmark.toRow(location, now, zone, showChapter = index.chapterCount > 1),
          group = GroupPosition(position, sorted.size),
        )
      }
    }
  }
}

private fun storyOrder(
  index: BookIndex,
  located: List<Pair<Bookmark, BookLocation>>,
  now: Instant,
  zone: ZoneId,
): List<BookmarkListItem> {
  val current = index.current
  val byChapter = located
    .sortedWith(compareBy<Pair<Bookmark, BookLocation>> { it.second.bookPosition }.thenBy { it.first.addedAt })
    .groupBy { it.second.chapterNumber }
  val chapterNumbers = (byChapter.keys + listOfNotNull(current?.chapterNumber)).toSortedSet()
  return buildList {
    chapterNumbers.forEach { chapterNumber ->
      val inChapter = byChapter[chapterNumber].orEmpty()
      val youAreHereAt = if (current != null && current.chapterNumber == chapterNumber) {
        inChapter.count { it.second.bookPosition <= current.bookPosition }
      } else {
        null
      }
      val count = inChapter.size + if (youAreHereAt != null) 1 else 0
      if (index.chapterCount > 1) {
        val name = inChapter.firstOrNull()?.second?.chapterName ?: current?.takeIf { it.chapterNumber == chapterNumber }?.chapterName
        add(BookmarkListItem.ChapterHeader(number = chapterNumber, name = name))
      }
      var position = 0
      inChapter.forEachIndexed { indexInChapter, (bookmark, location) ->
        if (indexInChapter == youAreHereAt) {
          add(youAreHere(current!!, GroupPosition(position++, count)))
        }
        add(
          BookmarkListItem.Row(
            bookmark = bookmark.toRow(location, now, zone, showChapter = false),
            group = GroupPosition(position++, count),
          ),
        )
      }
      if (youAreHereAt == inChapter.size) {
        add(youAreHere(current!!, GroupPosition(position, count)))
      }
    }
  }
}

private fun youAreHere(
  current: BookLocation,
  group: GroupPosition,
) = BookmarkListItem.YouAreHere(time = current.time, percent = current.percent, group = group)

internal fun Bookmark.toRow(
  location: BookLocation,
  now: Instant,
  zone: ZoneId,
  showChapter: Boolean,
): BookmarkRowViewState {
  return BookmarkRowViewState(
    id = id,
    kind = kind,
    setBySleepTimer = setBySleepTimer,
    note = title?.trim()?.takeIf { it.isNotEmpty() },
    chapterNumber = location.chapterNumber,
    chapterName = location.chapterName,
    time = location.time,
    percent = location.percent,
    savedAt = savedAtLabel(addedAt, now, zone, setBySleepTimer),
    showChapter = showChapter,
  )
}

internal fun categoryCounts(bookmarks: List<Bookmark>): List<CategoryCount> {
  val counts = bookmarks.groupingBy { it.category }.eachCount()
  return BookmarkCategory.entries.mapNotNull { category ->
    counts[category]?.let { CategoryCount(category, it) }
  }
}

internal fun bookBar(
  index: BookIndex,
  bookmarks: List<Bookmark>,
  playbackSpeed: Float,
): BookBarViewState? {
  val current = index.current ?: return null
  val pinned = bookmarks
    .mapNotNull { bookmark -> index.locate(bookmark.chapterId, bookmark.time)?.let { bookmark to it } }
    .sortedBy { it.second.bookPosition }
  val remaining = ((index.duration - current.bookPosition) / playbackSpeed.coerceAtLeast(0.1F)).toLong()
  return BookBarViewState(
    segments = index.segments,
    currentSegment = current.chapterNumber - 1,
    progress = current.progress,
    percent = current.percent,
    remaining = formatTime(remaining.coerceAtLeast(0L)),
    pins = pinned.map { (bookmark, location) ->
      BookBarPin(position = location.progress, kind = bookmark.kind, setBySleepTimer = bookmark.setBySleepTimer)
    },
    pinKeys = pinned.map { (bookmark, _) -> bookmark.id.value.toString() },
  )
}
