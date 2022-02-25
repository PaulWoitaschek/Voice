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
import java.time.Instant

class BookRepoBenchmark {

  @Rule
  @JvmField
  val benchmark = BenchmarkRule()

  @Test
  fun firstCollectionWithWarmup() = benchmark.measureRepeated {
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
    repo.warmupEnabled = true

    runBlocking {
      repo.flow().first()
    }

    runWithTimingDisabled {
      db.close()
    }
  }

  @Test
  fun firstCollectionWithoutWarmup() = benchmark.measureRepeated {
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
    repo.warmupEnabled = false

    runBlocking {
      repo.flow().first()
    }

    runWithTimingDisabled {
      db.close()
    }
  }

  @Test
  fun insertWithValidatingBookContent() = benchmark.measureRepeated {
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
      val contentRepo = BookContentRepo(db.bookContentDao())
      contentRepo.validateBookContent = true
      BookRepository(ChapterRepo(db.chapterDao()), contentRepo)
    }
    val bookIds = runWithTimingDisabled {
      runBlocking {
        repo.flow().first().map { it.id }
      }
    }

    runBlocking {
      bookIds.forEach {
        repo.updateBook(it) { it.copy(lastPlayedAt = Instant.now()) }
      }
    }

    runWithTimingDisabled {
      db.close()
    }
  }

  @Test
  fun insertWithoutValidatingBookContent() = benchmark.measureRepeated {
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
      val contentRepo = BookContentRepo(db.bookContentDao())
      contentRepo.validateBookContent = false
      BookRepository(ChapterRepo(db.chapterDao()), contentRepo)
    }
    val bookIds = runWithTimingDisabled {
      runBlocking {
        repo.flow().first().map { it.id }
      }
    }

    runBlocking {
      bookIds.forEach {
        repo.updateBook(it) { it.copy(lastPlayedAt = Instant.now()) }
      }
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
