package voice.core.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding
import voice.core.data.repo.internals.getLong
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.getStringOrNull
import voice.core.data.repo.internals.mapRows
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

// The sleep timer no longer sets bookmarks. The nights it marked move into the listening history, unless the listener
// wrote a note on them.
@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
public class Migration61 : IncrementalMigration(61) {

  override fun migrate(db: SupportSQLiteDatabase) {
    val bookmarks = db.query("SELECT * FROM bookmark2 WHERE setBySleepTimer = 1").mapRows {
      SleepBookmark(
        id = getString("id"),
        bookId = getString("bookId"),
        chapterId = getString("chapterId"),
        time = getLong("time"),
        addedAt = runCatching { Instant.parse(getString("addedAt")) }.getOrNull(),
        title = getStringOrNull("title"),
      )
    }
    bookmarks.forEach { bookmark ->
      val addedAt = bookmark.addedAt
      if (bookmark.title.isNullOrBlank() && addedAt != null) {
        if (!db.hasSleepTimerEventNear(bookmark.bookId, addedAt)) {
          db.insert(
            "listening_event",
            SQLiteDatabase.CONFLICT_FAIL,
            ContentValues().apply {
              put("bookId", bookmark.bookId)
              put("type", "SleepTimerSet")
              put("source", "SleepTimer")
              put("atMillis", addedAt.toEpochMilli())
              put("chapterId", bookmark.chapterId)
              put("time", bookmark.time)
            },
          )
        }
        db.delete("bookmark2", "id = ?", arrayOf(bookmark.id))
      } else {
        db.update(
          "bookmark2",
          SQLiteDatabase.CONFLICT_FAIL,
          ContentValues().apply { put("setBySleepTimer", false) },
          "id = ?",
          arrayOf(bookmark.id),
        )
      }
    }
  }

  private fun SupportSQLiteDatabase.hasSleepTimerEventNear(
    bookId: String,
    at: Instant,
  ): Boolean {
    val margin = 2.minutes.inWholeMilliseconds
    val millis = at.toEpochMilli()
    return query(
      "SELECT 1 FROM listening_event WHERE bookId = ? AND type = 'SleepTimerSet' AND atMillis BETWEEN ? AND ? LIMIT 1",
      arrayOf<Any>(bookId, millis - margin, millis + margin),
    ).use { it.moveToFirst() }
  }

  private class SleepBookmark(
    val id: String,
    val bookId: String,
    val chapterId: String,
    val time: Long,
    val addedAt: Instant?,
    val title: String?,
  )
}
