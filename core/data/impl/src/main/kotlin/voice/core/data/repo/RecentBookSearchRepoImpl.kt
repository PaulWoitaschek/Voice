package voice.core.data.repo

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import kotlinx.coroutines.flow.Flow
import voice.core.data.repo.internals.dao.RecentBookSearchDao

@ContributesBinding(AppScope::class)
public class RecentBookSearchRepoImpl
internal constructor(private val dao: RecentBookSearchDao) : RecentBookSearchRepo {

  override fun flow(): Flow<List<String>> = dao.recentBookSearches()

  override suspend fun add(query: String) {
    dao.add(query)
  }

  override suspend fun delete(query: String) {
    dao.delete(query)
  }

  override suspend fun clear() {
    dao.clear()
  }
}
