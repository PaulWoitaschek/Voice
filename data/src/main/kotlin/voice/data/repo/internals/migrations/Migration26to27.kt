package voice.data.repo.internals.migrations

import android.annotation.SuppressLint
import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.anvil.annotations.ContributesMultibinding
import voice.common.AppScope
import voice.data.repo.internals.moveToNextLoop
import javax.inject.Inject

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Migration::class,
)
class Migration26to27
@Inject constructor() : IncrementalMigration(26) {

  @SuppressLint("Recycle")
  override fun migrate(db: SupportSQLiteDatabase) {
    val copyBookTableName = "TABLE_BOOK_COPY"
    db.execSQL("DROP TABLE IF EXISTS $copyBookTableName")
    db.execSQL("ALTER TABLE TABLE_BOOK RENAME TO $copyBookTableName")
    db.execSQL(
      """CREATE TABLE TABLE_BOOK (
      |BOOK_ID INTEGER PRIMARY KEY AUTOINCREMENT,
      |BOOK_JSON TEXT NOT NULL,
      |LAST_TIME_BOOK_WAS_ACTIVE INTEGER NOT NULL,
      |BOOK_ACTIVE INTEGER NOT NULL)
      """.trimMargin(),
    )

    val cursor = db.query(copyBookTableName, arrayOf("BOOK_JSON"))
    cursor.moveToNextLoop {
      val cv = ContentValues()
      cv.put("BOOK_JSON", getString(0))
      cv.put("BOOK_ACTIVE", 1)
      cv.put("LAST_TIME_BOOK_WAS_ACTIVE", System.currentTimeMillis())
      db.insert("TABLE_BOOK", SQLiteDatabase.CONFLICT_FAIL, cv)
    }
  }
}
