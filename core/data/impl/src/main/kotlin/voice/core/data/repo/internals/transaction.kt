package voice.core.data.repo.internals

import androidx.sqlite.db.SupportSQLiteDatabase

@IgnorableReturnValue
internal inline fun <T> SupportSQLiteDatabase.transaction(
  exclusive: Boolean = true,
  body: SupportSQLiteDatabase.() -> T,
): T {
  if (exclusive) {
    beginTransaction()
  } else {
    beginTransactionNonExclusive()
  }
  try {
    val result = body()
    setTransactionSuccessful()
    return result
  } finally {
    endTransaction()
  }
}
