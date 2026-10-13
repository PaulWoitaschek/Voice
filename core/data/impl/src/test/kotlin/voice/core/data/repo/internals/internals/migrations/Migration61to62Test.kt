package voice.core.data.repo.internals.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Rule
import org.junit.runner.RunWith
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.allMigrations
import voice.core.data.repo.internals.getLong
import voice.core.data.repo.internals.getString
import voice.core.data.repo.internals.getStringOrNull
import voice.core.data.repo.internals.mapRows
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals

@RunWith(AndroidJUnit4::class)
class Migration61to62Test {

  @Rule
  @JvmField
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDb::class.java,
  )

  private val night = Instant.parse("2026-10-11T21:40:00Z")

  @Test
  fun `a sleep timer bookmark moves into the history`() {
    val migrated = migrate {
      insertBookmark(id = "a", title = null, setBySleepTimer = true, time = 4_000)
    }

    assertEquals(expected = emptyList(), actual = migrated.bookmarkIds())
    assertEquals(
      expected = listOf(Event(type = "SleepTimerSet", source = "SleepTimer", atMillis = night.toEpochMilli(), time = 4_000)),
      actual = migrated.events(),
    )
  }

  @Test
  fun `a sleep timer bookmark with a note stays as a bookmark`() {
    val migrated = migrate {
      insertBookmark(id = "a", title = "Mira's theory", setBySleepTimer = true)
    }

    assertEquals(expected = listOf("a"), actual = migrated.bookmarkIds())
    assertEquals(expected = emptyList(), actual = migrated.events())
  }

  @Test
  fun `a night the history already has isn't added twice`() {
    val migrated = migrate {
      insertBookmark(id = "a", title = null, setBySleepTimer = true)
      insertSleepTimerEvent(atMillis = night.toEpochMilli() + 1_000)
    }

    assertEquals(expected = emptyList(), actual = migrated.bookmarkIds())
    assertEquals(expected = 1, actual = migrated.events().size)
  }

  @Test
  fun `a sleep timer bookmark from an unknown time is gone without a night`() {
    val migrated = migrate {
      insertBookmark(id = "a", title = null, setBySleepTimer = true, addedAt = "yesterday")
    }

    assertEquals(expected = emptyList(), actual = migrated.bookmarkIds())
    assertEquals(expected = emptyList(), actual = migrated.events())
  }

  @Test
  fun `a deleted sleep timer bookmark can't be restored as a bookmark`() {
    val migrated = migrate {
      insertDeletedBookmarkEvent(setBySleepTimer = true)
      insertDeletedBookmarkEvent(setBySleepTimer = false)
    }

    assertEquals(expected = listOf("BookmarkDeleted"), actual = migrated.events().map { it.type })
  }

  @Test
  fun `other bookmarks stay as they are`() {
    val migrated = migrate {
      insertBookmark(id = "a", title = null, setBySleepTimer = false)
    }

    assertEquals(expected = listOf("a"), actual = migrated.bookmarkIds())
    assertEquals(expected = emptyList(), actual = migrated.events())
  }

  private fun migrate(fill: SupportSQLiteDatabase.() -> Unit): SupportSQLiteDatabase {
    helper.createDatabase(DB_NAME, 61).use { it.fill() }
    return helper.runMigrationsAndValidate(DB_NAME, 62, true, *allMigrations())
  }

  private fun SupportSQLiteDatabase.insertBookmark(
    id: String,
    title: String?,
    setBySleepTimer: Boolean,
    addedAt: String = night.toString(),
    time: Long = 1_000,
  ) {
    insert(
      "bookmark2",
      SQLiteDatabase.CONFLICT_FAIL,
      ContentValues().apply {
        put("bookId", "book")
        put("chapterId", "chapter")
        put("title", title)
        put("time", time)
        put("addedAt", addedAt)
        put("setBySleepTimer", setBySleepTimer)
        put("id", id)
      },
    )
  }

  private fun SupportSQLiteDatabase.insertSleepTimerEvent(atMillis: Long) {
    insert(
      "listening_event",
      SQLiteDatabase.CONFLICT_FAIL,
      ContentValues().apply {
        put("bookId", "book")
        put("type", "SleepTimerSet")
        put("source", "SleepTimer")
        put("atMillis", atMillis)
        put("chapterId", "chapter")
        put("time", 1_000L)
      },
    )
  }

  private fun SupportSQLiteDatabase.insertDeletedBookmarkEvent(setBySleepTimer: Boolean) {
    insert(
      "listening_event",
      SQLiteDatabase.CONFLICT_FAIL,
      ContentValues().apply {
        put("bookId", "book")
        put("type", "BookmarkDeleted")
        put("source", "App")
        put("atMillis", night.toEpochMilli())
        put("chapterId", "chapter")
        put("time", 1_000L)
        put("bookmarkSetBySleepTimer", setBySleepTimer)
      },
    )
  }

  private fun SupportSQLiteDatabase.bookmarkIds() = query("SELECT id FROM bookmark2").mapRows { getString("id") }

  private fun SupportSQLiteDatabase.events() = query("SELECT * FROM listening_event")
    .mapRows {
      Event(
        type = getString("type"),
        source = getString("source"),
        atMillis = getLong("atMillis"),
        time = getLong("time"),
        chapterId = getString("chapterId"),
        value = getStringOrNull("value"),
      )
    }

  private data class Event(
    val type: String,
    val source: String,
    val atMillis: Long,
    val time: Long,
    val chapterId: String = "chapter",
    val value: String? = null,
  )

  private companion object {
    const val DB_NAME = "testDb"
  }
}
