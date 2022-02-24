package voice.benchmark

import androidx.benchmark.junit4.BenchmarkRule
import androidx.benchmark.junit4.measureRepeated
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Rule
import org.junit.Test
import voice.app.book
import voice.data.repo.BookContentRepo
import voice.data.repo.BookRepository
import voice.data.repo.ChapterRepo
import voice.data.repo.internals.AppDb

class BookRepoBenchmark {

  @Rule
  @JvmField
  val benchmark = BenchmarkRule()

  @Test
  fun firstCollection() = benchmark.measureRepeated {
    runWithTimingDisabled {
      val db = database()
      db.clearAllTables()

      val chapterDao = db.chapterDao()
      val contentDao = db.bookContentDao()

      runBlocking {
        repeat(100) {
          val book = book()
          contentDao.insert(book.content)
          book.chapters.forEach {
            chapterDao.insert(it)
          }
        }
      }
      db.close()
    }
    val db = runWithTimingDisabled {
      database()
    }
    val repo = runWithTimingDisabled {
      BookRepository(ChapterRepo(db.chapterDao()), BookContentRepo(db.bookContentDao()))
    }

    runBlocking {
      repo.flow().first()
    }

    runWithTimingDisabled {
      db.close()
    }
  }

  private fun database(): AppDb {
    return Room.databaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java, "benchmarkDb")
      .build()
  }
}
