package voice.core.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import org.json.JSONObject

@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
@Inject
public class Migration25to26 : IncrementalMigration(25) {

  override fun migrate(db: SupportSQLiteDatabase) {
    // get all books
    val cursor = db.query(
      "TABLE_BOOK",
      arrayOf("BOOK_ID", "BOOK_JSON"),
    )
    val allBooks = ArrayList<JSONObject>(cursor.count)
    cursor.use {
      while (it.moveToNext()) {
        val content = it.getString(1)
        val book = JSONObject(content)
        book.put("id", it.getLong(0))
        allBooks.add(book)
      }
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
