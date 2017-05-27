package de.ph1b.audiobook.persistence.internals.migrations

import android.database.sqlite.SQLiteDatabase
import org.json.JSONObject
import java.util.*

/**
 * A previous version caused empty books to be added. So we delete them now.
 *
 * @author Paul Woitaschek
 */
class Migration25to26 : Migration {

  override fun migrate(db: SQLiteDatabase) {
    // get all books
    val cursor = db.query("TABLE_BOOK",
        arrayOf("BOOK_ID", "BOOK_JSON"),
        null, null, null, null, null)
    val allBooks = ArrayList<JSONObject>(cursor.count)
    try {
      while (cursor.moveToNext()) {
        val content = cursor.getString(1)
        val book = JSONObject(content)
        book.put("id", cursor.getLong(0))
        allBooks.add(book)
      }
    } finally {
      cursor.close()
    }

    // delete empty books
    for (b in allBooks) {
      val chapters = b.getJSONArray("chapters")
      if (chapters.length() == 0) {
        db.delete("TABLE_BOOK", "BOOK_ID" + "=?", arrayOf(b.get("id").toString()))
      }
    }
  }
}