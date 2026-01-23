package voice.core.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import org.json.JSONObject
import voice.core.logging.api.Logger
import java.io.File

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
public class Migration28to29 : IncrementalMigration(28) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.query("TABLE_BOOK", arrayOf("BOOK_JSON", "BOOK_ID"))
      .use { cursor ->
        while (cursor.moveToNext()) {
          val book = JSONObject(cursor.getString(0))
          val chapters = book.getJSONArray("chapters")
          for (i in 0 until chapters.length()) {
            val chapter = chapters.getJSONObject(i)
            val fileName = File(chapter.getString("path")).name
            val dotIndex = fileName.lastIndexOf(".")
            val chapterName = if (dotIndex > 0) {
              fileName.substring(0, dotIndex)
            } else {
              fileName
            }
            chapter.put("name", chapterName)
          }
          val cv = ContentValues()
          Logger.d("so saving book=$book")
          cv.put("BOOK_JSON", book.toString())
          db.update(
            "TABLE_BOOK",
            SQLiteDatabase.CONFLICT_FAIL,
            cv,
            "BOOK_ID" + "=?",
            arrayOf(cursor.getLong(1).toString()),
          )
        }
      }
  }
}
