package voice.core.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.binding

// an auto migration drops the columns before its spec could copy them
@ContributesIntoSet(
  scope = AppScope::class,
  binding = binding<Migration>(),
)
public class Migration67 : IncrementalMigration(67) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.execSQL("DROP TABLE bookSearchFts")
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `customMetadata` (`bookId` TEXT NOT NULL, `name` TEXT, `author` TEXT, `cover` TEXT,
      PRIMARY KEY(`bookId`))
      """
        .trimIndent(),
    )
    db.execSQL(
      """
      INSERT INTO customMetadata (bookId, name, cover)
      SELECT id, customName, customCover FROM content2
      WHERE customName IS NOT NULL OR customCover IS NOT NULL
      """
        .trimIndent(),
    )

    // dropping a column needs Android 14
    db.execSQL(
      """
      CREATE TABLE IF NOT EXISTS `_new_content2` (`id` TEXT NOT NULL, `playbackSpeed` REAL NOT NULL,
      `skipSilence` INTEGER NOT NULL, `isActive` INTEGER NOT NULL, `lastPlayedAt` TEXT NOT NULL, `addedAt` TEXT NOT NULL,
      `chapters` TEXT NOT NULL, `currentChapter` TEXT NOT NULL, `positionInChapter` INTEGER NOT NULL,
      `gain` REAL NOT NULL DEFAULT 0, `name` TEXT NOT NULL, `author` TEXT, `album` TEXT, `title` TEXT, `genre` TEXT,
      `narrator` TEXT, `series` TEXT, `part` TEXT, `cover` TEXT, `metadataFileLastModified` TEXT, PRIMARY KEY(`id`))
      """
        .trimIndent(),
    )
    // without the modification time, the next scan reads each book's first file again, so its
    // series and part arrive
    val columns =
      "id, playbackSpeed, skipSilence, isActive, lastPlayedAt, addedAt, chapters, currentChapter, " +
        "positionInChapter, gain, name, author, album, title, genre, narrator, series, part, cover"
    db.execSQL("INSERT INTO _new_content2 ($columns) SELECT $columns FROM content2")
    // takes the triggers that kept the search table up to date along
    db.execSQL("DROP TABLE content2")
    db.execSQL("ALTER TABLE _new_content2 RENAME TO content2")
  }
}
