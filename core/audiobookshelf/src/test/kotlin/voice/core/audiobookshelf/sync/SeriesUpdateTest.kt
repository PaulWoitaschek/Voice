package voice.core.audiobookshelf.sync

import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import voice.core.audiobookshelf.MemoryBookRepository
import voice.core.audiobookshelf.MemoryDataStore
import voice.core.audiobookshelf.ServerEdit
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.AbsBookMetadata
import voice.core.audiobookshelf.api.AbsSeries
import voice.core.audiobookshelf.api.BatchGetResponse
import voice.core.audiobookshelf.audiobookshelfJson
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.CustomMetadata
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class SeriesUpdateTest {

  private val server = MockWebServer()
  private lateinit var account: Account
  private val bookId = BookId("abs://item/item")

  @Before
  fun setUp() {
    server.start()
    account = Account(
      serverUrl = server.url("/").toString(),
      userId = "user",
      username = "paul",
      accessToken = "access",
      refreshToken = "refresh",
      libraryIds = listOf("library"),
      serverVersion = "2.37.1",
    )
  }

  @After
  fun tearDown() {
    server.close()
  }

  @Test
  fun `the first series is replaced, the others stay`() = runTest {
    val series = listOf(AbsSeries("The Expanse", "1"), AbsSeries("Space Operas", "4"))
    val repo = MemoryBookRepository(book(series))
    enqueueItem(series)
    server.enqueue(MockResponse(code = 200))

    val result = seriesUpdate(repo).update(bookId, series = " Expanse ", part = " 2 ")

    assertEquals(ServerEdit.Saved, result)
    assertEquals("/api/items/batch/get", server.takeRequest(1, TimeUnit.SECONDS)!!.url.encodedPath)
    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("PATCH", request.method)
    assertEquals("/api/items/item/media", request.url.encodedPath)
    assertEquals(
      """{"metadata":{"series":[{"name":"Expanse","sequence":"2"},{"name":"Space Operas","sequence":"4"}]}}""",
      request.body!!.utf8(),
    )
    assertEquals("Expanse", repo.get(bookId)!!.series)
    assertEquals("2", repo.get(bookId)!!.part)
  }

  @Test
  fun `the series the book shows is replaced, also when the server has it last`() = runTest {
    val series = listOf(AbsSeries("Space Operas", "4"), AbsSeries("The Expanse", "1"))
    val book = book(series).update { it.copy(metadata = it.metadata.copy(series = "The Expanse", part = "1")) }
    enqueueItem(series)
    server.enqueue(MockResponse(code = 200))

    val result = seriesUpdate(MemoryBookRepository(book)).update(bookId, series = "Expanse", part = "1")

    assertEquals(ServerEdit.Saved, result)
    server.takeRequest(1, TimeUnit.SECONDS)
    assertEquals(
      """{"metadata":{"series":[{"name":"Expanse","sequence":"1"},{"name":"Space Operas","sequence":"4"}]}}""",
      server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8(),
    )
  }

  @Test
  fun `the series the book shows is replaced, also when the server spells it differently`() = runTest {
    val series = listOf(AbsSeries("Space Operas", "4"), AbsSeries("The Expanse", "1"))
    val book = book(series).update { it.copy(metadata = it.metadata.copy(series = "the expanse", part = "1")) }
    enqueueItem(series)
    server.enqueue(MockResponse(code = 200))

    val result = seriesUpdate(MemoryBookRepository(book)).update(bookId, series = "the expanse", part = "2")

    assertEquals(ServerEdit.Saved, result)
    server.takeRequest(1, TimeUnit.SECONDS)
    assertEquals(
      """{"metadata":{"series":[{"name":"the expanse","sequence":"2"},{"name":"Space Operas","sequence":"4"}]}}""",
      server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8(),
    )
  }

  @Test
  fun `a series the book doesn't show yet stays`() = runTest {
    val repo = MemoryBookRepository(book(emptyList()))
    enqueueItem(listOf(AbsSeries("Space Operas", "4")))
    server.enqueue(MockResponse(code = 200))

    val result = seriesUpdate(repo).update(bookId, series = "The Expanse", part = "1")

    assertEquals(ServerEdit.Saved, result)
    server.takeRequest(1, TimeUnit.SECONDS)
    assertEquals(
      """{"metadata":{"series":[{"name":"The Expanse","sequence":"1"},{"name":"Space Operas","sequence":"4"}]}}""",
      server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8(),
    )
  }

  @Test
  fun `an empty series takes the book out of the first one`() = runTest {
    val series = listOf(AbsSeries("The Expanse", "1"))
    val repo = MemoryBookRepository(book(series))
    enqueueItem(series)
    server.enqueue(MockResponse(code = 200))

    val result = seriesUpdate(repo).update(bookId, series = " ", part = "1")

    assertEquals(ServerEdit.Saved, result)
    server.takeRequest(1, TimeUnit.SECONDS)
    assertEquals("""{"metadata":{"series":[]}}""", server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8())
    assertEquals(null, repo.get(bookId)!!.series)
    assertEquals(null, repo.get(bookId)!!.part)
  }

  @Test
  fun `another series with the new name isn't sent twice`() = runTest {
    val series = listOf(AbsSeries("Expanse"), AbsSeries("the expanse", "3"))
    enqueueItem(series)
    server.enqueue(MockResponse(code = 200))

    val result = seriesUpdate(MemoryBookRepository(book(series))).update(bookId, series = "The Expanse", part = "3")

    assertEquals(ServerEdit.Saved, result)

    server.takeRequest(1, TimeUnit.SECONDS)
    assertEquals(
      """{"metadata":{"series":[{"name":"The Expanse","sequence":"3"}]}}""",
      server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8(),
    )
  }

  @Test
  fun `an account that can't change books is told so, and the book stays`() = runTest {
    val series = listOf(AbsSeries("The Expanse", "1"))
    val repo = MemoryBookRepository(book(series))
    enqueueItem(series)
    server.enqueue(MockResponse(code = 403))

    val result = seriesUpdate(repo).update(bookId, series = "Expanse", part = "")

    assertEquals(ServerEdit.NotAllowed, result)
    assertEquals("The Expanse", repo.get(bookId)!!.series)
  }

  @Test
  fun `a server that can't be reached fails`() = runTest {
    val repo = MemoryBookRepository(book(emptyList()))
    server.close()

    assertEquals(ServerEdit.Failed, seriesUpdate(repo).update(bookId, series = "Expanse", part = ""))
  }

  private fun enqueueItem(series: List<AbsSeries>) {
    val response = BatchGetResponse(listOf(item(series)))
    server.enqueue(MockResponse(code = 200, body = audiobookshelfJson.encodeToString(response)))
  }

  private fun seriesUpdate(repo: MemoryBookRepository) = SeriesUpdate(
    http = AudiobookshelfHttp(AccountStore(MemoryDataStore<Account?>(account)), mockk { every { versionName } returns "1.0.0" }),
    accountStore = AccountStore(MemoryDataStore<Account?>(account)),
    bookRepository = repo,
  )

  private fun item(series: List<AbsSeries>) = item(
    tracks = listOf(track(1, 0.0, 60.0, "a.mp3")),
    metadata = AbsBookMetadata(title = "Leviathan Wakes", series = series),
  )

  private fun book(series: List<AbsSeries>): Book {
    val item = item(series)
    val chapters = item.chapters()
    val content = item.toBookContent(chapters, existing = null)
    return Book(content, chapters, CustomMetadata(content.id))
  }
}
