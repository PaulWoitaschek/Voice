package voice.data.repo.internals.migrations

import android.annotation.SuppressLint
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import voice.data.repo.internals.moveToNextLoop

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@SuppressLint("Recycle")
@Inject
class Migration30to31 : IncrementalMigration(30) {

  override fun migrate(db: SupportSQLiteDatabase) {
    // book keys
    val bookIdColumn = "bookId"
    val tableBook = "tableBooks"
    val tableChapters = "tableChapters"

    db.query(tableBook, arrayOf(bookIdColumn)).moveToNextLoop {
      val bookId = getLong(0)

      var chapterCount = 0
      val chapterCursor = db.query(
        SupportSQLiteQueryBuilder.builder(tableChapters)
          .selection("$bookIdColumn=?", arrayOf(bookId))
          .create(),
      )
      chapterCursor.moveToNextLoop {
        chapterCount++
      }
      if (chapterCount == 0) {
        db.delete(tableBook, "$bookIdColumn=?", arrayOf(bookId.toString()))
      }
    }
  }
}
