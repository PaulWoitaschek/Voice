package voice.core.audiobookshelf.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import voice.core.audiobookshelf.MemoryBookRepository
import voice.core.audiobookshelf.MemoryDataStore
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.account.SyncedBookmarksStore
import voice.core.audiobookshelf.api.AbsBookmark
import voice.core.audiobookshelf.api.AbsChapter
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.Book
import voice.core.data.Bookmark
import voice.core.data.repo.BookmarkRepo
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals

class BookmarkSyncTest {

  private val server = MockWebServer()
  private lateinit var account: Account

  private val item = item(
    tracks = listOf(track(1, 0.0, 60.0, "a.mp3"), track(2, 60.0, 60.0, "b.mp3")),
    chapters = listOf(AbsChapter(0.0, 60.0, "One"), AbsChapter(60.0, 120.0, "Two")),
    id = "item",
  )
  private val chapters = item.chapters()
  private val book = Book(item.toBookContent(chapters, existing = null), chapters)
  private val synced = MemoryDataStore<Map<String, String>>(emptyMap())
  private val bookmarkRepo = mockk<BookmarkRepo>(relaxUnitFun = true) {
    coEvery { bookmarks(any()) } returns emptyList()
  }

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
  fun `a bookmark set here goes to the server, named after its chapter`() = runTest {
    hasBookmarks(bookmark(chapter = 1, timeMs = 20_000, title = null))
    server.enqueue(MockResponse(code = 200))

    bookmarkSync().sync(account, serverBookmarks = emptyList())

    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("POST", request.method)
    assertEquals("/api/me/item/item/bookmark", request.url.encodedPath)
    val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
    assertEquals(80.0, body.getValue("time").jsonPrimitive.double)
    assertEquals("Two", body.getValue("title").jsonPrimitive.content)
    assertEquals(mapOf("item@80" to "Two"), synced.data.first())
    // the bookmark here stays untitled
    coVerify(exactly = 0) {
      bookmarkRepo.addBookmark(any())
      bookmarkRepo.deleteBookmark(any())
    }
  }

  @Test
  fun `a bookmark from the server shows up here`() = runTest {
    bookmarkSync().sync(account, listOf(AbsBookmark("item", "From the server", 30.0, createdAt = 1_000)))

    val added = slot<Bookmark>()
    coVerify(exactly = 1) { bookmarkRepo.addBookmark(capture(added)) }
    val bookmark = added.captured
    assertEquals(chapters[0].id, bookmark.chapterId)
    assertEquals(30_000, bookmark.time)
    assertEquals("From the server", bookmark.title)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `a bookmark deleted here is deleted on the server`() = runTest {
    synced.updateData { mapOf("item@30" to "Old") }
    server.enqueue(MockResponse(code = 200))

    bookmarkSync().sync(account, listOf(AbsBookmark("item", "Old", 30.0)))

    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("DELETE", request.method)
    assertEquals("/api/me/item/item/bookmark/30", request.url.encodedPath)
    assertEquals(emptyMap(), synced.data.first())
  }

  @Test
  fun `a bookmark deleted on the server is deleted here`() = runTest {
    synced.updateData { mapOf("item@30" to "Old") }
    val bookmark = bookmark(chapter = 0, timeMs = 30_000, title = "Old")
    hasBookmarks(bookmark)

    bookmarkSync().sync(account, serverBookmarks = emptyList())

    coVerify { bookmarkRepo.deleteBookmark(bookmark.id) }
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `a title changed on the server comes over`() = runTest {
    synced.updateData { mapOf("item@30" to "Old") }
    val bookmark = bookmark(chapter = 0, timeMs = 30_000, title = "Old")
    hasBookmarks(bookmark)

    bookmarkSync().sync(account, listOf(AbsBookmark("item", "New", 30.0)))

    coVerify { bookmarkRepo.addBookmark(bookmark.copy(title = "New")) }
    assertEquals(mapOf("item@30" to "New"), synced.data.first())
  }

  @Test
  fun `when both sides renamed a bookmark, the title of this device wins`() = runTest {
    synced.updateData { mapOf("item@30" to "Old") }
    hasBookmarks(bookmark(chapter = 0, timeMs = 30_000, title = "Here"))
    server.enqueue(MockResponse(code = 200))

    bookmarkSync().sync(account, listOf(AbsBookmark("item", "There", 30.0)))

    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("PATCH", request.method)
    assertEquals("Here", Json.parseToJsonElement(request.body!!.utf8()).jsonObject.getValue("title").jsonPrimitive.content)
    coVerify(exactly = 0) {
      bookmarkRepo.addBookmark(any())
      bookmarkRepo.deleteBookmark(any())
    }
    assertEquals(mapOf("item@30" to "Here"), synced.data.first())
  }

  @Test
  fun `a bookmark another app set at a fraction of a second is deleted at its exact time`() = runTest {
    synced.updateData { mapOf("item@30" to "Old") }
    server.enqueue(MockResponse(code = 200))

    bookmarkSync().sync(account, listOf(AbsBookmark("item", "Old", 30.6)))

    assertEquals("/api/me/item/item/bookmark/30.6", server.takeRequest(1, TimeUnit.SECONDS)!!.url.encodedPath)
  }

  private fun hasBookmarks(vararg bookmarks: Bookmark) {
    coEvery { bookmarkRepo.bookmarks(book.content) } returns bookmarks.toList()
  }

  private fun bookmarkSync(): BookmarkSync {
    return BookmarkSync(
      http = AudiobookshelfHttp(
        AccountStore(MemoryDataStore<Account?>(account)),
        mockk { every { versionName } returns "1.0.0" },
      ),
      bookRepository = MemoryBookRepository(book),
      bookmarkRepo = bookmarkRepo,
      syncedBookmarksStore = SyncedBookmarksStore(synced),
    )
  }

  private fun bookmark(
    chapter: Int,
    timeMs: Long,
    title: String?,
  ) = Bookmark(
    bookId = book.id,
    chapterId = chapters[chapter].id,
    title = title,
    time = timeMs,
    addedAt = Instant.EPOCH,
    id = Bookmark.Id.random(),
  )
}
