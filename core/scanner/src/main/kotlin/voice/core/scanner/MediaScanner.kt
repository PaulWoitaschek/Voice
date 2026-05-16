package voice.core.scanner

import dev.zacsweers.metro.Inject
import voice.core.data.BookId
import voice.core.data.folders.FolderType
import voice.core.data.isAudioFile
import voice.core.data.repo.BookContentRepo
import voice.core.documentfile.CachedDocumentFile
import voice.core.documentfile.walk
import voice.core.logging.api.Logger

@Inject
internal class MediaScanner(
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

    // Walk each candidate book exactly once and remember the audio files we find.
    // Reused below for the permission probe, ordering and chapter parsing instead
    // of walking the SAF tree three separate times.
    val booksWithAudioFiles: List<Pair<CachedDocumentFile, List<CachedDocumentFile>>> = files.map { file ->
      val audioFiles = if (file.isAudioFile()) {
        listOf(file)
      } else {
        file.walk().filter { it.isAudioFile() }.toList()
      }
      file to audioFiles
    }

    val probeFile = booksWithAudioFiles.asSequence()
      .flatMap { it.second.asSequence() }
      .firstOrNull { it.uri.authority == "com.android.externalstorage.documents" }
    if (probeFile != null) {
      if (deviceHasPermissionBug.checkForBugAndSet(probeFile)) {
        Logger.w("Device has permission bug, aborting scan! Probed $probeFile")
        return
      }
    }

    booksWithAudioFiles
      .sortedBy { it.second.size }
      .forEach { (file, audioFiles) ->
        scan(file, audioFiles)
      }
  }

  private suspend fun scan(
    file: CachedDocumentFile,
    audioFiles: List<CachedDocumentFile>,
  ) {
    val parseResult = chapterParser.parse(audioFiles)
    val chapters = parseResult.chapters
    if (chapters.isEmpty()) return

    val content = bookParser.parseAndStore(chapters, file, parseResult.firstFileMetadata)

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
