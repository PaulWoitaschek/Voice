package voice.core.data.repo

import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ListeningEvent

public interface ListeningHistoryRepo {

  /**
   * Newest first.
   */
  public fun events(bookId: BookId): Flow<List<ListeningEvent>>

  /**
   * Stores the event, unless listening history is turned off.
   */
  public suspend fun add(event: ListeningEvent)

  public suspend fun clear()

  public suspend fun removeExpired()
}
