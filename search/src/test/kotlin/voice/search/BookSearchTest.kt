package voice.search

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import voice.common.BookId
import voice.data.Book
import voice.data.repo.BookContentRepo
import voice.data.repo.BookRepository
import voice.data.repo.ChapterRepo
import voice.data.repo.internals.AppDb
import java.util.UUID

@RunWith(AndroidJUnit4::class)
class BookSearchTest {

  @Suppress("SpellCheckingInspection")
  private val commendatore = book(
    name = "Die Ermordung des Commendatore",
    author = "Haruki Murakami",
  )

  private val watchtower = book(
    name = "All Along the Watchtower",
    author = "Jimi Hendrix",
  )

  private val unicorns = book(
    name = "All Along the Unicorns",
    author = "Jimi Hendrix",
  )

  @Test
  fun `search complete name`() = test {
    expectSearchResult("Die Ermordung des Commendatore", commendatore)
  }

  @Test
  fun `search single word in name`() = test {
    expectSearchResult("Ermordung", commendatore)
  }

  @Test
  fun `search partial word in name from beginning with whitespace`() = test {
    expectSearchResult("   Ermord  ", commendatore)
  }

  @Test
  fun `search single word in author`() = test {
    expectSearchResult("Murakami", commendatore)
  }

  @Test
  fun `search partial word in name from beginning`() = test {
    expectSearchResult("Ermord", commendatore)
  }

  @Test
  fun `multiple matches on author`() = test {
    expectSearchResult("Jimi", watchtower, unicorns)
  }

  @Test
  fun `multiple matches on title`() = test {
    expectSearchResult("along", watchtower, unicorns)
  }

  @Test
  fun `sql reserved chars do not crash`() = test {
    search("-Ermordung")
    search("\"")
    search("**")
    search("--")
    search("-*\"--*\"\"")
  }

  private fun test(run: suspend TestBase.() -> Unit) {
    val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .build()
    val repo = BookRepository(
      chapterRepo = ChapterRepo(db.chapterDao()),
      contentRepo = BookContentRepo(db.bookContentDao()),
    )
    val search = BookSearch(
      dao = db.bookContentDao(),
      repo = repo,
    )

    val testBase = TestBase(search)

    suspend fun addBook(book: Book) {
      db.bookContentDao().insert(book.content)
      book.chapters.forEach {
        db.chapterDao().insert(it)
      }
    }

    runTest {
      addBook(commendatore)
      addBook(watchtower)
      addBook(unicorns)

      // this ensures that inactive books are never accounted for
      addBook(commendatore.withNewIdAndInactive())
      addBook(watchtower.withNewIdAndInactive())
      addBook(unicorns.withNewIdAndInactive())

      testBase.run()
      db.close()
    }
  }
}

private fun Book.withNewIdAndInactive(): Book {
  return update {
    it.copy(
      id = BookId(UUID.randomUUID().toString()),
      isActive = false,
    )
  }
}

private class TestBase(private val search: BookSearch) {

  suspend fun expectSearchResult(query: String, vararg expected: Book) {
    val result = search.search(query)
    result shouldContainExactly expected.toList()
  }

  suspend fun search(query: String) {
    search.search(query)
  }
}
