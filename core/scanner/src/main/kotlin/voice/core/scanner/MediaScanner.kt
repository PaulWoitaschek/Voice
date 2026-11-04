package voice.core.scanner

import dev.zacsweers.metro.Inject
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.sync.withPermit
import voice.core.data.BookId
import voice.core.data.fitIntoOneBook
import voice.core.data.folders.FolderType
import voice.core.data.isAudioFile
import voice.core.data.isRemote
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.ChapterRepo
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.walk
import voice.core.logging.api.Logger

@Inject
internal class MediaScanner(
  private val contentRepo: BookContentRepo,
  private val chapterParser: ChapterParser,
  private val bookParser: BookParser,
  private val deviceHasPermissionBug: DeviceHasStoragePermissionBug,
  private val progressCarryOver: ProgressCarryOver,
  private val chapterRepo: ChapterRepo,
) {

  suspend fun scan(folders: Map<FolderType, List<CachedDocumentFile>>) {
    val files = folders.flatMap { (folderType, files) ->
      when (folderType) {
        FolderType.SingleFile,
        FolderType.SingleFolder -> {
          files
        }
        FolderType.Root -> {
          files.flatMap { file ->
            file.children
          }
        }
        FolderType.Author -> {
          files.flatMap { folder ->
            folder.children.flatMap { author ->
              if (author.isFile) {
                listOf(author)
              } else {
                author.children
              }
            }
          }
        }
      }
    }

    val bookIds = files.map { BookId(it.uri) }
    // before the books found again become active, so they can be told apart from the books still in
    // the library
    val previousBooks =
      PreviousBooks(
        library = contentRepo.all().filterNot { it.id.isRemote },
        scanned = bookIds.toSet(),
        chapterRepo = chapterRepo,
      )
    contentRepo.setAllInactiveExcept(bookIds)

    val probeFile = folders.values.flatten().findProbeFile()
    if (probeFile != null) {
      if (deviceHasPermissionBug.checkForBugAndSet(probeFile)) {
        Logger.w("Device has permission bug, aborting scan!")
        return
      }
    }

    // carrying progress over moves bookmarks and history between books, so one book at a time
    val carryOverLock = Mutex()
    val parallelBooks = Semaphore(5)
    coroutineScope {
      files.forEach { file ->
        launch { parallelBooks.withPermit { scan(file, previousBooks, carryOverLock) } }
      }
    }
  }

  private fun List<CachedDocumentFile>.findProbeFile(): CachedDocumentFile? {
    return asSequence()
      .flatMap { it.walk() }
      .firstOrNull { child ->
        child.isAudioFile() && child.uri.authority == "com.android.externalstorage.documents"
      }
  }

  private suspend fun scan(
    file: CachedDocumentFile,
    previousBooks: PreviousBooks,
    carryOverLock: Mutex,
  ) {
    val parseResult = chapterParser.parse(file)
    val chapters = parseResult.chapters
    if (chapters.isEmpty()) return
    val chapterIds = chapters.map { it.id }
    if (!chapterIds.fitIntoOneBook()) {
      Logger.nonFatal(
        IllegalStateException(
          "Skipping a book with ${chapters.size} chapters, too many for one book",
        ),
      )
      return
    }

    val content = bookParser.parseAndStore(chapters, file, parseResult.firstChapterMetadata)

    val currentChapterGone = content.currentChapter !in chapterIds
    val currentChapter = if (currentChapterGone) chapterIds.first() else content.currentChapter
    val positionInChapter = if (currentChapterGone) 0 else content.positionInChapter
    val updated = carryOverLock.withLock {
      progressCarryOver.carryOver(
        content =
          content.copy(
            chapters = chapterIds,
            currentChapter = currentChapter,
            positionInChapter = positionInChapter,
            isActive = true,
          ),
        previousBooks = previousBooks,
      )
    }
    if (content != updated) {
      validateIntegrity(updated, chapters)
      contentRepo.put(updated)
    }
  }
}
