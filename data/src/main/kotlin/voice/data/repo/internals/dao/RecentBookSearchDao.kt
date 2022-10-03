package voice.data.repo.internals.dao

import androidx.room.Dao
import androidx.room.Query

@Dao
abstract class RecentBookSearchDao {

  @Query("SELECT searchTerm FROM recentBookSearch")
  abstract suspend fun recentBookSearch(): List<String>

  @Query("DELETE FROM recentBookSearch WHERE searchTerm = :query")
  abstract suspend fun delete(query: String)

  @Query("INSERT OR REPLACE INTO recentBookSearch (searchTerm) VALUES (:query)")
  internal abstract suspend fun addRaw(query: String)

  suspend fun add(query: String) {
    addRaw(query)
    val recentSearch = recentBookSearch()
    if (recentSearch.size > LIMIT) {
      recentSearch.take(recentSearch.size - LIMIT)
        .forEach {
          delete(it)
        }
    }
  }

  companion object {
    const val LIMIT = 7
  }
}
