package voice.data.repo.internals.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.squareup.anvil.annotations.ContributesMultibinding
import voice.common.AppScope
import javax.inject.Inject
private const val TABLE_NAME = "tableBooks"

@ContributesMultibinding(
  scope = AppScope::class,
  boundType = Migration::class,
)
class Migration34to35
@Inject constructor() : IncrementalMigration(34) {

  override fun migrate(db: SupportSQLiteDatabase) {
    db.delete(TABLE_NAME, "bookId<=", arrayOf(-1))
  }
}
