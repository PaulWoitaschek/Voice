package voice.core.data.repo

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.runner.RunWith
import voice.core.data.repo.internals.AppDb
import voice.core.data.repo.internals.dao.RecentBookSearchDao
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(AndroidJUnit4::class)
class RecentBookSearchRepoImplTest {

  private val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
    .allowMainThreadQueries()
    .build()
  private val repo = RecentBookSearchRepoImpl(db.recentBookSearchDao())

  @After
  fun tearDown() {
    db.close()
  }

  private suspend fun recentSearches(): List<String> = repo.flow().first()

  @Test
  fun `add delete replace`() = runTest {
    assertTrue(recentSearches().isEmpty())

    repo.add("cats")
    assertEquals(expected = listOf("cats"), actual = recentSearches())

    repo.add("dogs")
    assertEquals(expected = listOf("dogs", "cats"), actual = recentSearches())

    repo.add("unicorns")
    assertEquals(expected = listOf("unicorns", "dogs", "cats"), actual = recentSearches())

    repo.delete("dogs")
    assertEquals(expected = listOf("unicorns", "cats"), actual = recentSearches())

    repo.add("cats")
    assertEquals(expected = listOf("cats", "unicorns"), actual = recentSearches())
  }

  @Test
  fun `add over limit drops the oldest`() = runTest {
    val terms = (0..30).map { it.toString() }
    terms.forEach {
      repo.add(it)
    }

    assertEquals(expected = terms.takeLast(RecentBookSearchDao.LIMIT).reversed(), actual = recentSearches())
  }

  @Test
  fun `add replaces the same search in another case`() = runTest {
    repo.add("Stephen King")
    repo.add("dogs")
    repo.add("stephen king")
    assertEquals(expected = listOf("stephen king", "dogs"), actual = recentSearches())
  }

  @Test
  fun `clear removes all`() = runTest {
    repo.add("cats")
    repo.add("dogs")
    repo.clear()
    assertTrue(recentSearches().isEmpty())
  }
}
