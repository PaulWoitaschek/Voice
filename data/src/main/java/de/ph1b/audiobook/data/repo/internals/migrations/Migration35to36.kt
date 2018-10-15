package de.ph1b.audiobook.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import de.ph1b.audiobook.data.repo.internals.getFloat
import de.ph1b.audiobook.data.repo.internals.getInt
import de.ph1b.audiobook.data.repo.internals.getLong
import de.ph1b.audiobook.data.repo.internals.getString
import de.ph1b.audiobook.data.repo.internals.getStringOrNull
import de.ph1b.audiobook.data.repo.internals.mapRows
import de.ph1b.audiobook.data.repo.internals.transaction

private const val ID = "bookId"
private const val NAME = "bookName"
private const val AUTHOR = "bookAuthor"
private const val CURRENT_MEDIA_PATH = "bookCurrentMediaPath"
private const val PLAYBACK_SPEED = "bookSpeed"
private const val ROOT = "bookRoot"
private const val TIME = "bookTime"
private const val TYPE = "bookType"
private const val ACTIVE = "BOOK_ACTIVE"
private const val TABLE_NAME = "tableBooks"
private const val CREATE_TABLE = """
    CREATE TABLE $TABLE_NAME (
      $ID INTEGER PRIMARY KEY AUTOINCREMENT,
      $NAME TEXT NOT NULL,
      $AUTHOR TEXT,
      $CURRENT_MEDIA_PATH TEXT NOT NULL,
      $PLAYBACK_SPEED REAL NOT NULL,
      $ROOT TEXT NOT NULL,
      $TIME INTEGER NOT NULL,
      $TYPE TEXT NOT NULL,
      $ACTIVE INTEGER NOT NULL DEFAULT 1
    )
  """

class Migration35to36 : IncrementalMigration(35) {

  override fun migrate(db: SupportSQLiteDatabase) {
    val entries = db.query(TABLE_NAME)
      .mapRows {
        Holder(
          getLong(ID),
          getString(NAME),
          getStringOrNull(AUTHOR),
          getString(CURRENT_MEDIA_PATH),
          getFloat(PLAYBACK_SPEED),
          getString(ROOT),
          getLong(TIME),
          getString(TYPE),
          getInt(ACTIVE)
        )
      }
    db.transaction {
      db.execSQL("DROP TABLE $TABLE_NAME")
      db.execSQL(CREATE_TABLE)
      entries.forEach {
        val cv = ContentValues().apply {
          put(ID, it.id)
          put(NAME, it.name)
          put(AUTHOR, it.author)
          put(CURRENT_MEDIA_PATH, it.path)
          put(PLAYBACK_SPEED, it.speed)
          put(ROOT, it.root)
          put(TIME, it.time)
          put(TYPE, it.type)
          put(ACTIVE, it.active)
        }
        db.insert(TABLE_NAME, SQLiteDatabase.CONFLICT_FAIL, cv)
      }
    }
  }

  data class Holder(
    val id: Long,
    val name: String,
    val author: String?,
    val path: String,
    val speed: Float,
    val root: String,
    val time: Long,
    val type: String,
    val active: Int
  )
}
