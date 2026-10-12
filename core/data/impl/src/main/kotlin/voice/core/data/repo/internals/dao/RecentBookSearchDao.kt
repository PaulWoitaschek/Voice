package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

@Dao
public abstract class RecentBookSearchDao {

  /**
   * The recent searches, oldest first.
   */
  @Query("SELECT searchTerm FROM recentBookSearch ORDER BY rowid")
  public abstract suspend fun recentBookSearch(): List<String>

  /**
   * The recent searches, newest first.
   */
  @Query("SELECT searchTerm FROM recentBookSearch ORDER BY rowid DESC")
  public abstract fun recentBookSearches(): Flow<List<String>>

  @Query("DELETE FROM recentBookSearch WHERE searchTerm = :query")
  public abstract suspend fun delete(query: String)

  @Query("DELETE FROM recentBookSearch")
  public abstract suspend fun clear()

  @Query("INSERT OR REPLACE INTO recentBookSearch (searchTerm) VALUES (:query)")
  public abstract suspend fun addRaw(query: String)

  @Query("DELETE FROM recentBookSearch WHERE searchTerm = :query COLLATE NOCASE")
  public abstract suspend fun deleteIgnoringCase(query: String)

  /**
   * Adds the search as the newest one. It replaces the same search in another case, e.g. "stephen king" replaces
   * "Stephen King".
   */
  @Transaction
  public open suspend fun add(query: String) {
    deleteIgnoringCase(query)
    addRaw(query)
    val recentSearch = recentBookSearch()
    if (recentSearch.size > LIMIT) {
      recentSearch.take(recentSearch.size - LIMIT)
        .forEach {
          delete(it)
        }
    }
  }

  public companion object {
    public const val LIMIT: Int = 7
  }
}
