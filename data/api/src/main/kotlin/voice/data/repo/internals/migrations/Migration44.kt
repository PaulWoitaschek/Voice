package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import voice.data.repo.internals.getString

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@Inject
public class Migration44 : IncrementalMigration(44) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.query("SELECT * FROM bookSettings").use { bookSettingsCursor ->
      while (bookSettingsCursor.moveToNext()) {
        val bookId = bookSettingsCursor.getString("id")
        val currentFile = bookSettingsCursor.getString("currentFile")
        db.query("SELECT * FROM chapters WHERE bookId =?", arrayOf(bookId)).use { chapterCursor ->
          var chapterForCurrentFileFound = false
          while (!chapterForCurrentFileFound && chapterCursor.moveToNext()) {
            val chapterFile = chapterCursor.getString("file")
            if (chapterFile == currentFile) {
              chapterForCurrentFileFound = true
            }
          }
          if (!chapterForCurrentFileFound) {
            if (chapterCursor.moveToFirst()) {
              val firstChapterFile = chapterCursor.getString("file")
              val contentValues = ContentValues().apply {
                put("currentFile", firstChapterFile)
                put("positionInChapter", 0)
              }
              db.update("bookSettings", SQLiteDatabase.CONFLICT_FAIL, contentValues, "id =?", arrayOf(bookId))
            }
          }
        }
      }
    }
  }
}
