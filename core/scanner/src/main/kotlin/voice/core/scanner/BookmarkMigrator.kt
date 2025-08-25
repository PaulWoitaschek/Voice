package voice.core.scanner

import dev.zacsweers.metro.Inject
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.Chapter
import voice.core.data.legacy.LegacyBookMetaData
import voice.core.data.repo.internals.dao.BookmarkDao
import voice.core.data.repo.internals.dao.LegacyBookDao
import voice.core.data.runForMaxSqlVariableNumber
import voice.core.data.toUri

@Inject
internal class BookmarkMigrator(
  private val legacyBookDao: LegacyBookDao,
  private val bookmarkDao: BookmarkDao,
) {

  suspend fun migrate(
    migrationMetaData: LegacyBookMetaData,
    chapters: List<Chapter>,
    id: BookId,
  ) {
    legacyBookDao.chapters()
      .filter {
        it.bookId == migrationMetaData.id
      }
      .map { it.file }
      .runForMaxSqlVariableNumber { legacyBookDao.bookmarksByFiles(it) }
      .forEach { legacyBookmark ->
        val legacyChapter = legacyBookDao.chapters()
          .filter {
            it.bookId == migrationMetaData.id
          }
          .find { it.file == legacyBookmark.mediaFile }

        if (legacyChapter != null) {
          val matchingChapter = chapters.find {
            val chapterFilePath = it.id.toUri().filePath() ?: return@find false
            legacyChapter.file.absolutePath.endsWith(chapterFilePath)
          }
          if (matchingChapter != null) {
            bookmarkDao.addBookmark(
              Bookmark(
                bookId = id,
                addedAt = legacyBookmark.addedAt,
                chapterId = matchingChapter.id,
                id = Bookmark.Id.random(),
                setBySleepTimer = legacyBookmark.setBySleepTimer,
                time = legacyBookmark.time,
                title = legacyBookmark.title,
              ),
            )
          }
        }
      }
  }
}
