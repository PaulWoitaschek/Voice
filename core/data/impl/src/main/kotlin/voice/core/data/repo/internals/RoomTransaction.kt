package voice.core.data.repo.internals

import androidx.room.RoomDatabase
import kotlin.contracts.InvocationKind
import kotlin.contracts.contract
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal suspend inline fun <T> RoomDatabase.transaction(crossinline action: suspend () -> T): T {
  contract {
    callsInPlace(action, InvocationKind.EXACTLY_ONCE)
  }
  return withContext(Dispatchers.IO) {
    @Suppress("DEPRECATION") beginTransaction()
    try {
      action().also {
        @Suppress("DEPRECATION") setTransactionSuccessful()
      }
    } finally {
      @Suppress("DEPRECATION") endTransaction()
    }
  }
}
