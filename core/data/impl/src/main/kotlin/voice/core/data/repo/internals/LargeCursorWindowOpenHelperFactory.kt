package voice.core.data.repo.internals

import android.database.AbstractWindowedCursor
import android.database.Cursor
import android.database.CursorWindow
import android.database.CursorWindowAllocationException
import android.os.CancellationSignal
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.SupportSQLiteQuery
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory

// A book keeps all of its chapter ids in a single row. With a folder of ~10k files that row outgrows the default
// cursor window of 2 MB, and a row that doesn't fit into the window can never be read again.
// The window only allocates memory for the rows it actually holds, so only large results use more than before.
private const val CURSOR_WINDOW_SIZE_BYTES: Long = 16L * 1024 * 1024

internal class LargeCursorWindowOpenHelperFactory : SupportSQLiteOpenHelper.Factory {

  private val delegate = FrameworkSQLiteOpenHelperFactory()

  override fun create(configuration: SupportSQLiteOpenHelper.Configuration): SupportSQLiteOpenHelper {
    return LargeCursorWindowOpenHelper(delegate.create(configuration))
  }
}

private class LargeCursorWindowOpenHelper(private val delegate: SupportSQLiteOpenHelper) : SupportSQLiteOpenHelper by delegate {

  override val writableDatabase: SupportSQLiteDatabase
    get() = LargeCursorWindowDatabase(delegate.writableDatabase)

  override val readableDatabase: SupportSQLiteDatabase
    get() = LargeCursorWindowDatabase(delegate.readableDatabase)
}

private class LargeCursorWindowDatabase(private val delegate: SupportSQLiteDatabase) : SupportSQLiteDatabase by delegate {

  override fun query(query: String): Cursor = delegate.query(query).withLargeWindow()

  override fun query(
    query: String,
    bindArgs: Array<out Any?>,
  ): Cursor = delegate.query(query, bindArgs).withLargeWindow()

  override fun query(query: SupportSQLiteQuery): Cursor = delegate.query(query).withLargeWindow()

  override fun query(
    query: SupportSQLiteQuery,
    cancellationSignal: CancellationSignal?,
  ): Cursor = delegate.query(query, cancellationSignal).withLargeWindow()
}

private fun Cursor.withLargeWindow(): Cursor {
  if (this is AbstractWindowedCursor) {
    try {
      window = CursorWindow(null, CURSOR_WINDOW_SIZE_BYTES)
    } catch (_: CursorWindowAllocationException) {
      // the cursor falls back to the default window, which still reads everything that isn't oversized
    }
  }
  return this
}
