package voice.app.scanner

import voice.common.BookId
import voice.data.Bookmark
import voice.data.Chapter
import voice.data.legacy.LegacyBookMetaData
import voice.data.repo.internals.dao.BookmarkDao
import voice.data.repo.internals.dao.LegacyBookDao
import voice.data.runForMaxSqlVariableNumber
import voice.data.toUri
import javax.inject.Inject

class BookmarkMigrator
@Inject constructor(
  private val legacyBookDao: LegacyBookDao,
  private val bookmarkDao: BookmarkDao,
) {

  suspend fun migrate(migrationMetaData: LegacyBookMetaData, chapters: List<Chapter>, id: BookId) {
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
