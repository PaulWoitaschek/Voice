package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.anvil.annotations.ContributesMultibinding
import voice.common.AppScope
import voice.data.repo.internals.getLong
import voice.data.repo.internals.getString
import voice.data.repo.internals.mapRows
import voice.data.repo.internals.transaction
import javax.inject.Inject

private const val TABLE_NAME = "tableChapters"
private const val DURATION = "chapterDuration"
private const val NAME = "chapterName"
private const val PATH = "chapterPath"
private const val BOOK_ID = "bookId"
private const val LAST_MODIFIED = "lastModified"
private const val CREATE_TABLE = """
      CREATE TABLE $TABLE_NAME (
        $DURATION INTEGER NOT NULL,
        $NAME TEXT NOT NULL,
        $PATH TEXT NOT NULL,
        $BOOK_ID INTEGER NOT NULL,
        $LAST_MODIFIED INTEGER NOT NULL,
        FOREIGN KEY ($BOOK_ID) REFERENCES tableBooks (bookId)
      )
  """

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Migration::class,
)
class Migration36to37
@Inject constructor() : IncrementalMigration(36) {

  override fun migrate(db: SupportSQLiteDatabase) {
    val data = db.query("SELECT * FROM $TABLE_NAME").mapRows {
      Holder(getLong(DURATION), getString(NAME), getString(PATH), getLong(BOOK_ID))
    }

    db.transaction {
      db.execSQL("DROP TABLE $TABLE_NAME")
      db.execSQL(CREATE_TABLE)
      data.forEach {
        val cv = ContentValues().apply {
          put(DURATION, it.duration)
          put(NAME, it.name)
          put(BOOK_ID, it.bookId)
          put(PATH, it.path)
          put(LAST_MODIFIED, 0L)
        }
        db.insert(TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, cv)
      }
    }
  }

  private data class Holder(
    val duration: Long,
    val name: String,
    val path: String,
    val bookId: Long,
  )
}
