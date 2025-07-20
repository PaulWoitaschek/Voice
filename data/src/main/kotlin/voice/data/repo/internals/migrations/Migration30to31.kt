package voice.data.repo.internals.migrations

import android.annotation.SuppressLint
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteQueryBuilder
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import voice.common.AppScope
import voice.data.repo.internals.moveToNextLoop
import javax.inject.Inject

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@SuppressLint("Recycle")
class Migration30to31
@Inject constructor() : IncrementalMigration(30) {

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
