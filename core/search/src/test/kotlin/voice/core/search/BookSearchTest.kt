package voice.core.search

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Instant
import kotlin.uuid.Uuid
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.common.DispatcherProvider
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.MarkData
import voice.core.data.repo.BookContentRepoImpl
import voice.core.data.repo.BookRepositoryImpl
import voice.core.data.repo.ChapterRepoImpl
import voice.core.data.repo.CustomMetadataRepoImpl
import voice.core.data.repo.internals.AppDb

@RunWith(AndroidJUnit4::class)
class BookSearchTest {

  @Suppress("SpellCheckingInspection")
  private val commendatore =
    book(
      name = "Die Ermordung des Commendatore",
      author = "Haruki Murakami",
    )

  private val watchtower =
    book(
      name = "All Along the Watchtower",
      author = "Jimi Hendrix",
    )

  private val unicorns =
    book(
      name = "All Along the Unicorns",
      author = "Jimi Hendrix",
    )

  private val harryPotter1 =
    book(
      name = "Harry Potter and the Philosopher's Stone",
      author = "J. K. Rowling",
      genre = "Fantasy",
      narrator = "Stephen Fry",
      series = "Harry Potter",
      part = "1",
    )

  private val harryPotter2 =
    book(
      name = "Harry Potter and the Chamber of Secrets",
      author = "J. K. Rowling",
      genre = "Fantasy",
      narrator = "Stephen Fry",
      series = "Harry Potter",
      part = "2",
    )
  private val kingkiller1 =
    book(
      name = "The Name of the Wind",
      author = "Patrick Rothfuss",
      genre = "Fantasy",
      narrator = "Rupert Degas",
      series = "Kingkiller Chronicle",
      part = "1",
    )
  private val kingkiller25 =
    book(
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
    expectSearchResult("Jimi", unicorns, watchtower)
  }

  @Test
  fun `multiple matches on title`() = test {
    expectSearchResult("along", unicorns, watchtower)
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

  @Test
  fun `empty query finds nothing`() = test {
    expectSearchResult("")
    expectSearchResult("  ")
    expectSearchResult("**")
  }

  @Test
  fun `whole words beat word starts and titles beat authors`() {
    val wayOfKings = book(name = "The Way of Kings", author = "Brandon Sanderson")
    val fairyTale = book(name = "Fairy Tale", author = "Stephen King")
    val elfland = book(name = "The King of Elfland's Daughter", author = "Lord Dunsany")
    test(wayOfKings, fairyTale, elfland) {
      expectSearchResult("king", elfland, fairyTale, wayOfKings)
    }
  }

  @Test
  fun `books in progress and recently played come first`() {
    val notStarted = book(name = "Dune", time = 0)
    val playedLongAgo = book(name = "Dune Messiah", lastPlayedAt = Instant.fromEpochSeconds(1))
    val playedRecently = book(name = "Children of Dune", lastPlayedAt = Instant.fromEpochSeconds(2))
    test(notStarted, playedLongAgo, playedRecently) {
      expectSearchResult("dune", playedRecently, playedLongAgo, notStarted)
    }
  }

  @Test
  fun `matches of neighboring words are joined`() {
    val book = book(name = "The Way of Kings", author = "Brandon Sanderson")
    test(book) {
      assertEquals(
        expected = mapOf(BookSearchField.Author to listOf(0..16)),
        actual = search.search("brandon sanderson").single().matches,
      )
      assertEquals(
        expected = mapOf(BookSearchField.Title to listOf(0..2, 11..15)),
        actual = search.search("the kings").single().matches,
      )
    }
  }

  @Test
  fun `matches point to the matched words`() = test {
    val result = search.search("kingkiller 1").single()
    assertEquals(
      expected = mapOf(BookSearchField.Series to listOf(0..9, 21..21)),
      actual = result.matches,
    )

    val rothfuss = search.search("rothfuss slow").single()
    assertEquals(
      expected =
        mapOf(
          BookSearchField.Title to listOf(4..7),
          BookSearchField.Author to listOf(8..15),
          BookSearchField.Narrator to listOf(8..15),
        ),
      actual = rothfuss.matches,
    )
  }

  @Test
  fun `fields matching all words`() = test {
    val kingkiller = search.search("kingkiller 1").single()
    assertEquals(
      expected = setOf(BookSearchField.Series),
      actual = kingkiller.fieldsMatchingAllWords,
    )

    val rothfuss = search.search("rothfuss slow").single()
    assertEquals(expected = emptySet(), actual = rothfuss.fieldsMatchingAllWords)

    val fry = search.search("stephen fry").map { it.fieldsMatchingAllWords }.distinct().single()
    assertEquals(expected = setOf(BookSearchField.Narrator), actual = fry)
  }

  @Test
  fun `diacritics are ignored`() {
    val cafe = book(name = "Café Society", author = "Ana Müller")
    test(cafe) {
      expectSearchResult("cafe", cafe)
      expectSearchResult("CAFÉ", cafe)
      expectSearchResult("muller", cafe)
      assertEquals(
        expected = mapOf(BookSearchField.Title to listOf(0..3)),
        actual = search.search("cafe").single().matches,
      )
    }
  }

  @Test
  fun `the author the listener gave a book is found instead of the one from its files`() {
    val dune =
      book(name = "Dune", author = "Herbert").let {
        it.copy(custom = it.custom.copy(author = "Frank Herbert"))
      }
    test(dune) {
      expectSearchResult("frank", dune)
      assertEquals(
        expected = mapOf(BookSearchField.Author to listOf(0..4)),
        actual = search.search("frank").single().matches,
      )
    }
  }

  @Test
  fun `the series and part the listener gave a book are found`() {
    val interference =
      book(name = "Interference", author = "Sue Burke", series = "Signal", part = "1").let {
        it.copy(custom = it.custom.copy(series = "The Signal Saga", part = "2"))
      }
    test(interference) {
      expectSearchResult("signal saga 2", interference)
      expectSearchResult("signal 1")
    }
  }

  @Test
  fun `a book the listener edited is still found by the name and author from its files`() {
    val edited =
      book(name = "Track 01", author = "Dan Simmons").let {
        it.copy(custom = it.custom.copy(name = "Hyperion", author = "Simmons"))
      }
    val tracker = book(name = "Tracker")
    test(edited, tracker) {
      expectSearchResult("hyperion", edited)
      expectSearchResult("dan", edited)
      expectSearchResult("track", tracker, edited)
    }
  }

  @Test
  fun `diacritics are ignored in greek too`() {
    val greek = book(name = "Ελληνικά παραμύθια")
    test(greek) {
      expectSearchResult("ελληνικα", greek)
    }
  }

  @Test
  fun `hangul is found by its first syllables`() {
    val korean = book(name = "해리 포터와 마법사의 돌")
    test(korean) {
      assertEquals(
        expected = mapOf(BookSearchField.Title to listOf(0..1)),
        actual = search.search("해리").single().matches,
      )
    }
  }

  @Test
  fun `one typo per word is forgiven`() = test {
    expectSearchResult("harry pottr", harryPotter1, harryPotter2)
    expectSearchResult("ermrodung", commendatore)
    expectSearchResult("murakamy", commendatore)
    expectSearchResult("muraakami", commendatore)
    expectSearchResult("rotfuss slow", kingkiller25)
    expectSearchResult("murakmy")
    expectSearchResult("fyr")
    expectSearchResult("thex")
    assertEquals(
      expected =
        mapOf(BookSearchField.Title to listOf(0..11), BookSearchField.Series to listOf(0..11)),
      actual = search.search("harry pottr").first().matches,
    )
  }

  @Test
  fun `typos only count when nothing matches as typed`() {
    val king = book(name = "The Shining", author = "Stephen King")
    val lohse = book(name = "Der Hals der Giraffe", author = "Stephan Lohse")
    test(king, lohse) {
      expectSearchResult("stephin", lohse, king)
      expectSearchResult("stephen", king)
    }
  }

  private val redHeadedLeague =
    book(
      name = "The Adventures",
      author = "Arthur Conan Doyle",
      genre = null,
      narrator = null,
      chapters =
        listOf(chapter(name = "A Scandal in Bohemia"), chapter(name = "The Red-Headed League")),
    )

  @Test
  fun `red headed finds the book with the chapter The Red-Headed League and shows the chapter`() {
    test(redHeadedLeague) {
      expectSearchResult("red headed", redHeadedLeague)
      val result = search.search("red headed").single()
      assertEquals(expected = "The Red-Headed League", actual = result.chapter)
      assertEquals(
        expected = mapOf(BookSearchField.Chapters to listOf(4..6, 8..13)),
        actual = result.matches,
      )
      assertEquals(
        expected = setOf(BookSearchField.Chapters),
        actual = result.fieldsMatchingAllWords,
      )
    }
  }

  @Test
  fun `sherlock league finds Sherlock Holmes with the chapter The Red-Headed League`() {
    val sherlock =
      book(
        name = "Sherlock Holmes",
        chapters =
          listOf(chapter(name = "A Scandal in Bohemia"), chapter(name = "The Red-Headed League")),
      )
    test(sherlock, redHeadedLeague) {
      expectSearchResult("sherlock league", sherlock)
    }
  }

  @Test
  fun `red-heded finds the book with the chapter The Red-Headed League with one typo forgiven`() {
    test(redHeadedLeague) {
      expectSearchResult("red-heded", redHeadedLeague)
      assertEquals(
        expected = "The Red-Headed League",
        actual = search.search("red-heded").single().chapter,
      )
    }
  }

  @Test
  fun `league finds League of Legends first and then the other book with the chapter`() {
    val legends = book(name = "League of Legends", author = "Riot", genre = null, narrator = null)
    test(redHeadedLeague, legends) {
      expectSearchResult("league", legends, redHeadedLeague)
    }
  }

  @Test
  fun `a chapter ranks below the start of a word in any other field`() {
    val title = book(name = "Leaguers United", genre = null, narrator = null)
    val genre = book(name = "Unrelated", genre = "Leaguers", narrator = null)
    val chapter =
      book(
        name = "The Adventures",
        genre = null,
        narrator = null,
        chapters = listOf(chapter(name = "Intro"), chapter(name = "League")),
      )
    test(title, genre, chapter) {
      expectSearchResult("league", title, genre, chapter)
    }
  }

  @Test
  fun `league shows the first chapter that matches, The Red-Headed League`() {
    val book =
      book(
        name = "The Adventures",
        genre = null,
        narrator = null,
        chapters =
          listOf(chapter(name = "The Red-Headed League"), chapter(name = "The League of Five")),
      )
    test(book) {
      val result = search.search("league").single()
      assertEquals(expected = "The Red-Headed League", actual = result.chapter)
      assertEquals(
        expected = mapOf(BookSearchField.Chapters to listOf(15..20)),
        actual = result.matches,
      )
    }
  }

  @Test
  fun `the start of a word and a word without accents find a chapter`() {
    val book =
      book(
        name = "The Journey",
        genre = null,
        narrator = null,
        chapters = listOf(chapter(name = "Intro"), chapter(name = "Capítulo del mar")),
      )
    test(book) {
      assertEquals(expected = "Capítulo del mar", actual = search.search("capit").single().chapter)
      assertEquals(
        expected = "Capítulo del mar",
        actual = search.search("capitulo").single().chapter,
      )
    }
  }

  @Test
  fun `a typo in a title still finds the book when a chapter matches as typed`() {
    val dragon = book(name = "Dragon Rider", genre = null, narrator = null)
    val other =
      book(
        name = "The Adventures",
        genre = null,
        narrator = null,
        chapters = listOf(chapter(name = "Intro"), chapter(name = "Dragnet Night")),
      )
    test(dragon, other) {
      expectSearchResult("dragn", dragon, other)
    }
  }

  @Test
  fun `a chapter that matches as typed keeps typos out of other chapters`() {
    val night =
      book(
        name = "Watchmen",
        genre = null,
        narrator = null,
        chapters = listOf(chapter(name = "Intro"), chapter(name = "The Night Watch")),
      )
    val light =
      book(
        name = "Stories",
        genre = null,
        narrator = null,
        chapters = listOf(chapter(name = "Intro"), chapter(name = "Into the Light")),
      )
    val fight = book(name = "Fight Club", genre = null, narrator = null)
    test(night, light, fight) {
      expectSearchResult("night", fight, night)
    }
  }

  @Test
  fun `a book found by its title shows no chapter`() {
    val book =
      book(
        name = "Dune",
        genre = null,
        narrator = null,
        chapters = listOf(chapter(name = "Dune - Part 01"), chapter(name = "Dune - Part 02")),
      )
    test(book) {
      val result = search.search("dune").single()
      assertEquals(expected = null, actual = result.chapter)
      assertEquals(expected = setOf(BookSearchField.Title), actual = result.matches.keys)
      assertEquals(
        expected = setOf(BookSearchField.Title),
        actual = result.fieldsMatchingAllWords,
      )
    }
  }

  @Test
  fun `chapter marks are found by their names`() {
    val book =
      book(
        name = "The Adventures",
        genre = null,
        narrator = null,
        chapters =
          listOf(
            chapter(
              name = "adventures.m4b",
              marks =
                listOf(
                  MarkData(0, "A Scandal in Bohemia"),
                  MarkData(5000, "The Red-Headed League"),
                ),
            ),
          ),
      )
    test(book) {
      assertEquals(
        expected = "The Red-Headed League",
        actual = search.search("red headed").single().chapter,
      )
      expectSearchResult("m4b")
    }
  }

  @Test
  fun `escape finds a file without a title tag named 03 Escape mp3`() {
    val book =
      book(
        name = "The Journey",
        genre = null,
        narrator = null,
        chapters =
          listOf(
            chapter(name = "01 Arrival.mp3"),
            chapter(name = "02 Storm.mp3"),
            chapter(name = "03 Escape.mp3"),
          ),
      )
    test(book) {
      expectSearchResult("escape", book)
      val result = search.search("escape").single()
      assertEquals(expected = "Escape", actual = result.chapter)
      assertEquals(
        expected = mapOf(BookSearchField.Chapters to listOf(0..5)),
        actual = result.matches,
      )
    }
  }

  @Test
  fun `chapter finds nothing in a book with a chapter without a name`() {
    val book =
      book(
        name = "The Journey",
        genre = null,
        narrator = null,
        chapters =
          listOf(chapter(name = null), chapter(name = "Chapter 2"), chapter(name = "Track 03.mp3")),
      )
    test(book) {
      expectSearchResult("chapter")
      expectSearchResult("track")
    }
  }

  @Test
  fun `a changed book is found by what changed`() {
    val book =
      book(name = "Dune", chapters = listOf(chapter(name = "Arrakis"), chapter(name = "Spice")))
    test(book) {
      expectSearchResult("arrakis", book)
      val chapters = listOf(chapter(name = "Caladan"), chapter(name = "Spice"))
      putBook(
        Book(
          content =
            book.content.copy(
              chapters = chapters.map { it.id },
              currentChapter = chapters.first().id,
            ),
          chapters = chapters,
          custom = book.custom.copy(name = "Dune Messiah"),
        ),
      )
      expectSearchResult("arrakis")
      assertEquals(expected = "Caladan", actual = search.search("caladan").single().chapter)
      assertEquals(expected = "Dune Messiah", actual = search.search("messiah").single().book.name)
    }
  }

  private fun test(run: suspend TestBase.() -> Unit) =
    test(
      commendatore,
      watchtower,
      unicorns,
      harryPotter1,
      harryPotter2,
      kingkiller1,
      kingkiller25,
      run = run,
    )

  private fun test(
    vararg books: Book,
    run: suspend TestBase.() -> Unit,
  ) {
    val db =
      Room.inMemoryDatabaseBuilder(ApplicationProvider.getApplicationContext(), AppDb::class.java)
        .build()
    val contentRepo = BookContentRepoImpl(db.bookContentDao())
    val customMetadataRepo = CustomMetadataRepoImpl(db.customMetadataDao())
    val repo =
      BookRepositoryImpl(
        chapterRepo = ChapterRepoImpl(db.chapterDao()),
        contentRepo = contentRepo,
        customMetadataRepo = customMetadataRepo,
      )
    suspend fun addBook(book: Book) {
      db.bookContentDao().insert(book.content)
      customMetadataRepo.put(book.custom)
      book.chapters.forEach {
        db.chapterDao().insert(it)
      }
    }

    runTest {
      val search =
        BookSearch(
          repo,
          DispatcherProvider(
            coroutineContext,
            coroutineContext,
            coroutineContext,
            coroutineContext,
          ),
        )
      val testBase =
        TestBase(search) { book ->
          book.chapters.forEach { db.chapterDao().insert(it) }
          contentRepo.put(book.content)
          customMetadataRepo.put(book.custom)
        }
      books.forEach { addBook(it) }

      // this ensures that inactive books are never accounted for
      books.forEach { addBook(it.withNewIdAndInactive()) }

      testBase.run()
      db.close()
    }
  }
}

private fun Book.withNewIdAndInactive(): Book {
  val id = BookId(Uuid.random().toString())
  return copy(content = content.copy(id = id, isActive = false), custom = custom.copy(bookId = id))
}

private class TestBase(
  val search: BookSearch,
  val putBook: suspend (Book) -> Unit,
) {

  suspend fun expectSearchResult(
    query: String,
    vararg expected: Book,
  ) {
    val result = search.search(query).map { it.book }
    assertEquals(expected = expected.toList(), actual = result)
  }

  suspend fun search(query: String) {
    @Suppress("RETURN_VALUE_NOT_USED") search.search(query)
  }
}
