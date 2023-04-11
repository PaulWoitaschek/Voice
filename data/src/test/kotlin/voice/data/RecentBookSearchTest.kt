package voice.data

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import app.cash.turbine.test
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.flow.first
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

    dao.recentBookSearch().test {
      awaitItem().shouldBeEmpty()

      dao.add("cats")
      awaitItem().shouldContainExactly("cats")

      dao.add("dogs")
      awaitItem().shouldContainExactly("cats", "dogs")

      dao.add("unicorns")
      awaitItem().shouldContainExactly("cats", "dogs", "unicorns")

      dao.delete("dogs")
      awaitItem().shouldContainExactly("cats", "unicorns")

      dao.add("cats")
      awaitItem().shouldContainExactly("unicorns", "cats")
    }
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

    dao.recentBookSearch().first()
      .shouldContainExactly(terms.takeLast(RecentBookSearchDao.LIMIT))

    db.close()
  }
}
