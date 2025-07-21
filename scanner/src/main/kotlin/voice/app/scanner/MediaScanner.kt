package voice.app.scanner

import dev.zacsweers.metro.Inject
import voice.common.BookId
import voice.data.audioFileCount
import voice.data.folders.FolderType
import voice.data.isAudioFile
import voice.data.repo.BookContentRepo
import voice.documentfile.CachedDocumentFile
import voice.documentfile.walk
import voice.logging.core.Logger

@Inject
class MediaScanner(
  private val contentRepo: BookContentRepo,
  private val chapterParser: ChapterParser,
  private val bookParser: BookParser,
  private val deviceHasPermissionBug: DeviceHasStoragePermissionBug,
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
        FolderType.Author -> {
          files.flatMap { folder ->
            folder.children.flatMap { author ->
              if (author.isFile) {
                listOf(author)
              } else {
                author.children.flatMap {
                  author.children
                }
              }
            }
          }
        }
      }
    }

    contentRepo.setAllInactiveExcept(files.map { BookId(it.uri) })

    val probeFile = folders.values.flatten().findProbeFile()
    if (probeFile != null) {
      if (deviceHasPermissionBug.checkForBugAndSet(probeFile)) {
        Logger.w("Device has permission bug, aborting scan! Probed $probeFile")
        return
      }
    }

    files
      .sortedBy { it.audioFileCount() }
      .forEach { file ->
        scan(file)
      }
  }

  private fun List<CachedDocumentFile>.findProbeFile(): CachedDocumentFile? {
    return asSequence().flatMap { it.walk() }
      .firstOrNull { child ->
        child.isAudioFile() && child.uri.authority == "com.android.externalstorage.documents"
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
