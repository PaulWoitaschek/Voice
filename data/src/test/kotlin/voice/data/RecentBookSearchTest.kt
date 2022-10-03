package voice.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import voice.data.repo.internals.AppDb
import voice.data.repo.internals.dao.RecentBookSearchDao

@RunWith(AndroidJUnit4::class)
class RecentBookSearchTest {

  @Test
  fun `add delete replace`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .build()
    val dao = db.recentBookSearchDao()

    dao.recentBookSearch().shouldBeEmpty()

    dao.add("cats")
    dao.add("dogs")
    dao.add("unicorns")

    dao.recentBookSearch().shouldContainExactly("cats", "dogs", "unicorns")

    dao.delete("dogs")

    dao.recentBookSearch().shouldContainExactly("cats", "unicorns")

    dao.add("cats")

    dao.recentBookSearch().shouldContainExactly("unicorns", "cats")

    db.close()
  }

  @Test
  fun `add over limit replaces`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .build()
    val dao = db.recentBookSearchDao()

    val terms = (0..30).map { it.toString() }
    terms.forEach {
      dao.add(it)
    }

    dao.recentBookSearch().shouldContainExactly(terms.takeLast(RecentBookSearchDao.LIMIT))

    db.close()
  }
}
