package voice.data.repo.internals

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import voice.data.repo.internals.dao.RecentBookSearchDao

@RunWith(AndroidJUnit4::class)
class RecentBookSearchTest {

  @Test
  fun `add delete replace`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDb::class.java,
    )
      .build()

    with(db.recentBookSearchDao()) {
      recentBookSearch().shouldBeEmpty()

      add("cats")
      recentBookSearch().shouldContainExactly("cats")

      add("dogs")
      recentBookSearch().shouldContainExactly("cats", "dogs")

      add("unicorns")
      recentBookSearch().shouldContainExactly("cats", "dogs", "unicorns")

      delete("dogs")
      recentBookSearch().shouldContainExactly("cats", "unicorns")

      add("cats")
      recentBookSearch().shouldContainExactly("unicorns", "cats")
    }
    db.close()
  }

  @Test
  fun `add over limit replaces`() = runTest {
    val db = Room.inMemoryDatabaseBuilder(
      ApplicationProvider.getApplicationContext(),
      AppDb::class.java,
    )
      .build()
    val dao = db.recentBookSearchDao()

    val terms = (0..30).map { it.toString() }
    terms.forEach {
      dao.add(it)
    }

    dao.recentBookSearch()
      .shouldContainExactly(terms.takeLast(RecentBookSearchDao.Companion.LIMIT))

    db.close()
  }
}
