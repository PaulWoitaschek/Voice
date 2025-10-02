package voice.core.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
public abstract class RecentBookSearchDao {

  @Query("SELECT searchTerm FROM recentBookSearch")
  public abstract suspend fun recentBookSearch(): List<String>

  @Query("SELECT searchTerm FROM recentBookSearch")
  public abstract fun recentBookSearches(): Flow<List<String>>

  @Query("DELETE FROM recentBookSearch WHERE searchTerm = :query")
  public abstract suspend fun delete(query: String)

  @Query("INSERT OR REPLACE INTO recentBookSearch (searchTerm) VALUES (:query)")
  public abstract suspend fun addRaw(query: String)

  public suspend fun add(query: String) {
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
