package voice.migration.views

import voice.common.comparator.NaturalOrderComparator
import voice.data.legacy.LegacyBookmark
import voice.data.legacy.LegacyChapter
import voice.data.repo.internals.dao.LegacyBookDao
import java.io.File
import java.time.Instant

internal data class MigrationViewState(
  val items: List<Item>,
) {
  data class Item(
    val name: String,
    val currentChapter: String,
    val positionInChapterMs: Long,
    val root: String,
    val bookmarks: List<Bookmark>,
  ) {
    data class Bookmark(
      val chapter: String,
      val positionMs: Long,
      val title: String?,
      val addedAt: Instant?,
    )
  }
}

private data class LegacyChapterMark(
  val name: String,
  val startMs: Long,
  val endMs: Long
)

private val LegacyChapter.chapterMarks: List<LegacyChapterMark>
  get() = if (markData.isEmpty()) {
    listOf(LegacyChapterMark(name, 0L, duration))
  } else {
    val sorted = markData.sorted()
    sorted.mapIndexed { index, (startMs, name) ->
      val isFirst = index == 0
      val isLast = index == sorted.size - 1
      val start = if (isFirst) 0L else startMs
      val end = if (isLast) duration else sorted[index + 1].startMs - 1
      LegacyChapterMark(name = name, startMs = start, endMs = end)
    }
  }

private fun LegacyChapter.markForPosition(positionInChapterMs: Long): LegacyChapterMark {
  return chapterMarks.find { positionInChapterMs in it.startMs..it.endMs }
    ?: chapterMarks.firstOrNull { positionInChapterMs == it.endMs }
    ?: chapterMarks.first()
}


internal suspend fun migrationViewState(dao: LegacyBookDao): MigrationViewState {
  val allMetaData = dao.bookMetaData()
  val allSettings = dao.settings()
  val allChapters = dao.chapters()
  val allBookmarks = dao.bookmarks()

  val items = allMetaData.mapNotNull { metaData ->
    val settings = allSettings.find { it.id == metaData.id }
      ?: return@mapNotNull null

    val chapters = allChapters.filter { it.bookId == metaData.id }
      .sortedWith(LegacyChapterComparator)

    val currentChapter = chapters.find { settings.currentFile == it.file }
      ?: return@mapNotNull null

    val currentMark = currentChapter.markForPosition(settings.positionInChapter)
    val positionInMark = (settings.positionInChapter - currentMark.startMs)


    val bookmarks = allBookmarks
      .filter { bookmark ->
        chapters.any { chapter ->
          chapter.file == bookmark.mediaFile
        }
      }
      .sortedWith(LegacyBookmarkComparator)
      .filter { !it.setBySleepTimer }
      .mapNotNull bookmarkLoop@{ bookmark ->
        val bookmarkChapter = chapters.find { it.file == bookmark.mediaFile }
          ?: return@bookmarkLoop null
        val bookmarkMark = bookmarkChapter.markForPosition(bookmark.time)
        val bookmarkPositionInMark = (bookmark.time - currentMark.startMs)

        MigrationViewState.Item.Bookmark(
          chapter = bookmarkMark.name,
          positionMs = bookmarkPositionInMark,
          title = bookmark.title,
          addedAt = bookmark.addedAt,
        )
      }

    MigrationViewState.Item(
      name = metaData.name,
      currentChapter = settings.currentFile.absolutePath,
      root = metaData.root,
      positionInChapterMs = positionInMark,
      bookmarks = bookmarks,
    )
  }
  return MigrationViewState(items)
}

private object LegacyChapterComparator : Comparator<LegacyChapter> {
  override fun compare(o1: LegacyChapter, o2: LegacyChapter): Int = fileComparator.compare(o1.file, o2.file)
}

private object LegacyBookmarkComparator : Comparator<LegacyBookmark> {
  override fun compare(o1: LegacyBookmark, o2: LegacyBookmark): Int {
    val fileCompare = fileComparator.compare(o1.mediaFile, o2.mediaFile)
    if (fileCompare != 0) {
      return fileCompare
    }

    val timeCompare = o1.time.compareTo(o2.time)
    if (timeCompare != 0) return timeCompare

    val titleCompare = NaturalOrderComparator.stringComparator.compare(o1.title, o2.title)
    if (titleCompare != 0) return titleCompare

    return o1.id.compareTo(o2.id)
  }
}


private val fileComparator = Comparator<File> { lhs, rhs ->
  if (lhs == rhs) return@Comparator 0

  if (lhs.isDirectory && !rhs.isDirectory) {
    return@Comparator -1
  } else if (!lhs.isDirectory && rhs.isDirectory) {
    return@Comparator 1
  }

  val left = getFileWithParents(lhs)
  val right = getFileWithParents(rhs)

  val leftSize = left.size
  val rightSize = right.size

  // compare parents only and return if one differs
  var i = 0
  val toLeft = leftSize - 1
  val toRight = rightSize - 1
  while (i < toLeft && i < toRight) {
    val pl = left[i].name
    val pr = right[i].name
    if (pl != pr) {
      return@Comparator NaturalOrderComparator.stringComparator.compare(pl, pr)
    }
    i++
  }

  // if sizes are the same
  if (leftSize == rightSize) {
    NaturalOrderComparator.stringComparator.compare(lhs.name, rhs.name)
  } else {
    rightSize - leftSize
  }
}

private fun getFileWithParents(target: File): List<File> {
  val all = ArrayList<File>(10)
  var current: File? = target
  do {
    all.add(current!!)
    current = current.parentFile
  } while (current != null)
  return all.reversed()
}
