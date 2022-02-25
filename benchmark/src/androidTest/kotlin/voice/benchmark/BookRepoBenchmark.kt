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
import voice.data.Book
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
  fun flow() = runBenchmark { repo, _ ->
    repo.flow().first()
  }

  @Test
  fun update() = runBenchmark { repo, books ->
    books.forEach {
      repo.updateBook(it.id) { book ->
        book.copy(lastPlayedAt = Instant.now())
      }
    }
  }

  @Test
  fun update2() = runBenchmark { repo, books ->
    books.forEach {
      repo.updateBook2(it.id) { book ->
        book.copy(lastPlayedAt = Instant.now())
      }
    }
  }

  private inline fun runBenchmark(crossinline block: suspend (BookRepository, List<Book>) -> Unit) {
    val databaseName = "benchmarkDb"
    benchmark.measureRepeated {
      val books = runWithTimingDisabled {
        (0..100).map {
          book()
        }
      }

      runWithTimingDisabled {
        val db = Room.databaseBuilder(
          ApplicationProvider.getApplicationContext(),
          AppDb::class.java,
          databaseName
        )
          .build()
        db.clearAllTables()

        val chapterDao = db.chapterDao()
        val contentDao = db.bookContentDao()

        runBlocking {
          books.forEach { book ->
            contentDao.insert(book.content)
            book.chapters.forEach {
              chapterDao.insert(it)
            }
          }
        }
        db.close()
      }

      val freshDb = runWithTimingDisabled {
        Room.databaseBuilder(
          ApplicationProvider.getApplicationContext(),
          AppDb::class.java,
          databaseName
        )
          .build()
      }
      val repo = runWithTimingDisabled {
        BookRepository(ChapterRepo(freshDb.chapterDao()), BookContentRepo(freshDb.bookContentDao()))
      }

      runBlocking {
        block(repo, books)
      }

      runWithTimingDisabled {
        freshDb.close()
      }
    }
  }
}
