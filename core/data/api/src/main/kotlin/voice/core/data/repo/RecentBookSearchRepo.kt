package voice.core.data.repo

import kotlinx.coroutines.flow.Flow

public interface RecentBookSearchRepo {

  /**
   * The recent searches, newest first.
   */
  public fun flow(): Flow<List<String>>

  /**
   * Adds the search as the newest one. It replaces the same search in another case, e.g. "stephen king" replaces
   * "Stephen King".
   */
  public suspend fun add(query: String)

  public suspend fun delete(query: String)

  public suspend fun clear()
}
