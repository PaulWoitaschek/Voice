package voice.migration

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import voice.common.comparator.NaturalOrderComparator
import voice.common.formatTime
import voice.common.navigation.Navigator
import voice.data.legacy.LegacyBookMetaData
import voice.data.legacy.LegacyBookSettings
import voice.data.legacy.LegacyBookmark
import voice.data.legacy.LegacyChapter
import voice.data.legacy.LegacyChapterMark
import voice.data.repo.internals.dao.LegacyBookDao
import voice.migration.views.MigrationViewState
import java.io.File
import javax.inject.Inject

private const val COMMON_STORAGE_PREFIX = "/storage/emulated/0/"

class MigrationViewModel
@Inject constructor(
  private val dao: LegacyBookDao,
  private val navigator: Navigator,
) {

  @Composable
  internal fun viewState(): MigrationViewState {
    var showDeletionConfirmationDialog by remember {
      mutableStateOf(false)
    }
    val onDeleteClick = {
      showDeletionConfirmationDialog = true
    }
    val onDeletionAbort = {
      showDeletionConfirmationDialog = false
    }
    var items by remember {
      mutableStateOf(listOf<MigrationViewState.Item>())
    }
    val scope = rememberCoroutineScope()
    val onDeletionConfirm: () -> Unit = {
      showDeletionConfirmationDialog = false
      scope.launch {
        dao.deleteAll()
        items = emptyList()
      }
    }
    LaunchedEffect(Unit) {
      items = migrationItems()
    }

    return MigrationViewState(
      items = items,
      onDeleteClick = onDeleteClick,
      showDeletionConfirmationDialog = showDeletionConfirmationDialog,
      onDeletionConfirm = onDeletionConfirm,
      onDeletionAbort = onDeletionAbort,
    )
  }

  private suspend fun migrationItems(): List<MigrationViewState.Item> = withContext(Dispatchers.IO) {
    val migrationData = migrationData()
    migrationData.metaData.mapNotNull { metaData ->
      migrationItem(metaData, migrationData)
    }
  }

  internal fun onCloseClick() {
    navigator.goBack()
  }

  private suspend fun migrationData(): MigrationData {
    val metaData: List<LegacyBookMetaData> = dao.bookMetaData()
    val settings: List<LegacyBookSettings> = dao.settings()
    val chapters: List<LegacyChapter> = dao.chapters()
    val bookmarks: List<LegacyBookmark> = dao.bookmarks()
    return MigrationData(metaData, settings, chapters, bookmarks)
  }
}

private data class MigrationData(
  val metaData: List<LegacyBookMetaData>,
  val settings: List<LegacyBookSettings>,
  val chapters: List<LegacyChapter>,
  val bookmarks: List<LegacyBookmark>,
)

private fun migrationItem(
  metaData: LegacyBookMetaData,
  migrationData: MigrationData,
): MigrationViewState.Item? {
  val settings = migrationData.settings.find { it.id == metaData.id }
    ?: return null

  val chapters = migrationData.chapters.filter { it.bookId == metaData.id }
    .sortedWith(LegacyChapterComparator)

  val currentChapter = chapters.find { settings.currentFile == it.file }
    ?: return null

  val currentMark = currentChapter.markForPosition(settings.positionInChapter)
  val positionInMark = (settings.positionInChapter - currentMark.startMs)

  val bookmarks = migrationData.bookmarks
    .filter { bookmark ->
      chapters.any { chapter ->
        chapter.file == bookmark.mediaFile
      }
    }
    .sortedWith(LegacyBookmarkComparator)
    .filter { !it.setBySleepTimer }
    .mapNotNull { bookmark ->
      bookmark(chapters, bookmark, currentMark)
    }

  return if (bookmarks.isEmpty() &&
    chapters.indexOf(currentChapter) == 0 &&
    settings.positionInChapter == 0L
  ) {
    null
  } else {
    MigrationViewState.Item(
      name = metaData.name,
      root = metaData.root.removePrefix(COMMON_STORAGE_PREFIX),
      bookmarks = bookmarks,
      position = MigrationViewState.Position(
        currentChapter = currentMark.name,
        positionInChapter = formatTime(positionInMark),
        currentFile = currentChapter.name,
        positionInFile = formatTime(settings.positionInChapter),
      ),
    )
  }
}

private fun bookmark(
  chapters: List<LegacyChapter>,
  bookmark: LegacyBookmark,
  currentMark: LegacyChapterMark,
): MigrationViewState.Item.Bookmark? {
  val bookmarkChapter = chapters.find { it.file == bookmark.mediaFile }
  return if (bookmarkChapter == null) {
    null
  } else {
    val bookmarkMark = bookmarkChapter.markForPosition(bookmark.time)
    val bookmarkPositionInMark = (bookmark.time - currentMark.startMs)

    MigrationViewState.Item.Bookmark(
      position = MigrationViewState.Position(
        currentChapter = bookmarkMark.name,
        positionInChapter = formatTime(bookmarkPositionInMark),
        currentFile = bookmark.mediaFile.absolutePath.removePrefix(COMMON_STORAGE_PREFIX),
        positionInFile = formatTime(bookmark.time),
      ),
      title = bookmark.title,
      addedAt = bookmark.addedAt,
    )
  }
}

private object LegacyChapterComparator : Comparator<LegacyChapter> {
  override fun compare(
    o1: LegacyChapter,
    o2: LegacyChapter,
  ): Int {
    return fileComparator.compare(o1.file, o2.file)
  }
}

private object LegacyBookmarkComparator : Comparator<LegacyBookmark> {
  override fun compare(
    o1: LegacyBookmark,
    o2: LegacyBookmark,
  ): Int {
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
