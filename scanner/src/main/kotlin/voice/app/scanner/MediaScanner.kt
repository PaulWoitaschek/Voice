package voice.app.scanner

import voice.common.BookId
import voice.data.audioFileCount
import voice.data.folders.FolderType
import voice.data.repo.BookContentRepo
import voice.documentfile.CachedDocumentFile
import voice.logging.core.Logger
import javax.inject.Inject
import kotlin.time.measureTime

class MediaScanner
@Inject constructor(
  private val contentRepo: BookContentRepo,
  private val chapterParser: ChapterParser,
  private val bookParser: BookParser,
) {

  suspend fun scan(folders: Map<FolderType, List<CachedDocumentFile>>) {
    val files = folders.flatMap { (folderType, files) ->
      when (folderType) {
        FolderType.SingleFile, FolderType.SingleFolder -> {
          files
        }
        FolderType.Root -> {
          files.flatMap { file ->
            file.children
          }
        }
      }
    }

    contentRepo.setAllInactiveExcept(files.map { BookId(it.uri) })

    files
      .sortedBy { it.audioFileCount() }
      .forEach { file ->
        Logger.d("scanning $file")
        measureTime {
          scan(file)
        }.also {
          Logger.i("scan took $it for ${file.uri}")
        }
      }
  }

  private suspend fun scan(file: CachedDocumentFile) {
    val chapters = chapterParser.parse(file)
    if (chapters.isEmpty()) return

    val content = bookParser.parseAndStore(chapters, file)

    val chapterIds = chapters.map { it.id }
    val currentChapterGone = content.currentChapter !in chapterIds
    val currentChapter = if (currentChapterGone) chapterIds.first() else content.currentChapter
    val positionInChapter = if (currentChapterGone) 0 else content.positionInChapter
    val updated = content.copy(
      chapters = chapterIds,
      currentChapter = currentChapter,
      positionInChapter = positionInChapter,
      isActive = true,
    )
    if (content != updated) {
      validateIntegrity(updated, chapters)
      contentRepo.put(updated)
    }
  }
}
