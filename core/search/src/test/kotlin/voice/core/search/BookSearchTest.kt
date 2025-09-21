package voice.core.search

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.kotest.matchers.collections.shouldContainExactly
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookContentRepoImpl
import voice.core.data.repo.BookRepositoryImpl
import voice.core.data.repo.ChapterRepoImpl
import voice.core.data.repo.internals.AppDb
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

  private val harryPotter1 = book(
    name = "Harry Potter and the Philosopher's Stone",
    author = "J. K. Rowling",
    genre = "Fantasy",
    narrator = "Stephen Fry",
    series = "Harry Potter",
    part = "1",
  )

  private val harryPotter2 = book(
    name = "Harry Potter and the Chamber of Secrets",
    author = "J. K. Rowling",
    genre = "Fantasy",
    narrator = "Stephen Fry",
    series = "Harry Potter",
    part = "2",
  )
  private val kingkiller1 = book(
    name = "The Name of the Wind",
    author = "Patrick Rothfuss",
    genre = "Fantasy",
    narrator = "Rupert Degas",
    series = "Kingkiller Chronicle",
    part = "1",
  )
  private val kingkiller25 = book(
    name = "The Slow Regard of Silent Things",
    author = "Patrick Rothfuss",
    genre = "Fantasy",
    narrator = "Patrick Rothfuss",
    series = "Kingkiller Chronicle",
    part = "2.5",
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

  @Test
  fun `search for narrator, series and part`() = test {
    expectSearchResult("Harry Potter 1", harryPotter1)
    expectSearchResult("harry potter 1", harryPotter1)
    expectSearchResult("harry potter", harryPotter1, harryPotter2)
    expectSearchResult("kingkiller 1", kingkiller1)
    expectSearchResult("rupert degas 1", kingkiller1)
    expectSearchResult("kingkiller 2.5", kingkiller25)
    expectSearchResult("slow regard 2.5", kingkiller25)
    expectSearchResult("rothfuss 2.5", kingkiller25)
    expectSearchResult("kingkiller", kingkiller1, kingkiller25)
  }

  private fun test(run: suspend TestBase.() -> Unit) {
    val db = Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
      .build()
    val repo = BookRepositoryImpl(
      chapterRepo = ChapterRepoImpl(db.chapterDao()),
      contentRepo = BookContentRepoImpl(db.bookContentDao()),
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
      addBook(harryPotter1)
      addBook(harryPotter2)
      addBook(kingkiller1)
      addBook(kingkiller25)

      // this ensures that inactive books are never accounted for
      addBook(commendatore.withNewIdAndInactive())
      addBook(watchtower.withNewIdAndInactive())
      addBook(unicorns.withNewIdAndInactive())
      addBook(harryPotter1.withNewIdAndInactive())
      addBook(harryPotter2.withNewIdAndInactive())
      addBook(kingkiller1.withNewIdAndInactive())
      addBook(kingkiller25.withNewIdAndInactive())

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

  suspend fun expectSearchResult(
    query: String,
    vararg expected: Book,
  ) {
    val result = search.search(query)
    result shouldContainExactly expected.toList()
  }

  suspend fun search(query: String) {
    search.search(query)
  }
}
