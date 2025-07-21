package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding

private const val BOOK_TABLE_NAME = "tableBooks"
private const val BOOK_TIME = "bookTime"

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@Inject
class Migration39to40 : IncrementalMigration(39) {

  override fun migrate(db: SupportSQLiteDatabase) {
    val positionZeroContentValues = ContentValues().apply {
      put(BOOK_TIME, 0)
    }
    db.update(
      BOOK_TABLE_NAME,
      SQLiteDatabase.CONFLICT_FAIL,
      positionZeroContentValues,
      "$BOOK_TIME < ?",
      arrayOf(0),
    )
  }
}
