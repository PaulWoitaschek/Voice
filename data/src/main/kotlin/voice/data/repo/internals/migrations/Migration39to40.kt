package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.anvil.annotations.ContributesMultibinding
import voice.common.AppScope
import javax.inject.Inject

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Migration::class,
)
class Migration39to40
@Inject constructor() : IncrementalMigration(39) {

  private val BOOK_TABLE_NAME = "tableBooks"
  private val BOOK_TIME = "bookTime"

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
