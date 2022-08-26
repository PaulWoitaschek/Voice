package voice.data.repo.internals.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.anvil.annotations.ContributesMultibinding
import voice.common.AppScope
import voice.data.repo.internals.getInt
import voice.data.repo.internals.getString
import voice.data.repo.internals.moveToNextLoop
import java.time.Instant
import java.util.UUID
import javax.inject.Inject

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Migration::class,
)
class Migration49
@Inject constructor() : IncrementalMigration(49) {

  override fun migrate(db: SupportSQLiteDatabase) {
    with(db) {
      execSQL("ALTER TABLE bookmark RENAME TO bookmark_old")
      execSQL(
        """CREATE TABLE `bookmark`
        |(`file` TEXT NOT NULL,
        |`title` TEXT,
        |`time` INTEGER NOT NULL,
        |`addedAt` TEXT NOT NULL,
        |`setBySleepTimer` INTEGER NOT NULL,
        |`id` TEXT NOT NULL, PRIMARY KEY(`id`))
        """.trimMargin(),
      )
      query("SELECT * FROM bookmark_old").moveToNextLoop {
        val file = getString("file")
        val time = getInt("time")
        val title = getString("title")
        insert(
          "bookmark",
          SQLiteDatabase.CONFLICT_FAIL,
          ContentValues().apply {
            put("file", file)
            put("title", title)
            put("time", time)
            put("addedAt", Instant.EPOCH.toString())
            put("setBySleepTimer", false)
            put("id", UUID.randomUUID().toString())
          },
        )
      }
      execSQL("DROP TABLE bookmark_old")
    }
  }
}
