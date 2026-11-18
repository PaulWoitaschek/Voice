package voice.core.audiobookshelf.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import java.io.File
import kotlin.io.path.createTempFile
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import mockwebserver3.SocketEffect.CloseSocket
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import voice.core.audiobookshelf.MemoryDataStore
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.AbsLibrary
import voice.core.audiobookshelf.api.AbsLibraryItem
import voice.core.audiobookshelf.api.BatchGetRequest
import voice.core.audiobookshelf.api.BatchGetResponse
import voice.core.audiobookshelf.api.LibrariesResponse
import voice.core.audiobookshelf.api.LibraryItemsResponse
import voice.core.audiobookshelf.audiobookshelfJson
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.repo.ChapterRepo

class LibrarySyncTest {

  private val server = MockWebServer()
  private lateinit var accountStore: AccountStore

  @Before
  fun setUp() {
    server.start()
    accountStore =
      AccountStore(
        MemoryDataStore<Account?>(
          Account(
            serverUrl = server.url("/").toString(),
            userId = "user",
            username = "paul",
            accessToken = "access",
            refreshToken = "refresh",
            libraryIds = listOf("library"),
            serverVersion = "2.37.1",
          ),
        ),
      )
  }

  @After
  fun tearDown() {
    server.close()
  }

  @Test
  fun `a book that didn't change gets its library, and the libraries their names`() = runTest {
    val item = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")))
    val chapters = item.chapters()
    val stored = item.toBookContent(chapters, existing = null).copy(serverLibraryId = null)
    val contentRepo = contentRepo(stored)
    val chapterRepo =
      mockk<ChapterRepo> {
        coEvery { get(any<ChapterId>()) } answers { chapters.find { it.id == firstArg() } }
      }

    sync(item, contentRepo, chapterRepo)

    coVerify { contentRepo.put(listOf(stored.copy(serverLibraryId = "library"))) }
    assertEquals(mapOf("library" to "Kids"), accountStore.data.first()?.libraryNames)
  }

  @Test
  fun `a book the server flags gets its library and stays as it is`() = runTest {
    val item = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")))
    val stored = item.toBookContent(item.chapters(), existing = null).copy(serverLibraryId = null)
    val contentRepo = contentRepo(stored)

    sync(item.copy(isMissing = true), contentRepo, chapterRepo = mockk())

    coVerify { contentRepo.put(listOf(stored.copy(serverLibraryId = "library"))) }
  }

  @Test
  fun `a cover file a backup left out goes, so the book loads its cover from the server`() =
    runTest {
      val item = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")))
      val content = item.toBookContent(item.chapters(), existing = null)
      val stored = content.copy(metadata = content.metadata.copy(cover = File("missing.jpg")))
      val contentRepo = contentRepo(stored)

      sync(item, contentRepo, chapterRepo(item.chapters()))

      coVerify { contentRepo.put(listOf(content)) }
    }

  @Test
  fun `a cover file of an older version stays, so a downloaded book keeps it offline`() = runTest {
    val item = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")))
    val content = item.toBookContent(item.chapters(), existing = null)
    val cover = createTempFile().toFile()
    val contentRepo = contentRepo(content.copy(metadata = content.metadata.copy(cover = cover)))

    sync(item, contentRepo, chapterRepo(item.chapters()))

    assertTrue(cover.exists())
    coVerify(exactly = 0) { contentRepo.put(match<List<BookContent>> { it.isNotEmpty() }) }
  }

  @Test
  fun `a book that changed on the server drops its old cover file`() = runTest {
    val before = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")), updatedAt = 1_000)
    val content = before.toBookContent(before.chapters(), existing = null)
    val cover = createTempFile().toFile()
    val stored = content.copy(metadata = content.metadata.copy(cover = cover))
    val written = mutableListOf<List<BookContent>>()
    val contentRepo = contentRepo(stored)
    coEvery { contentRepo.put(capture(written)) } returns Unit
    val changed = before.copy(updatedAt = 2_000)
    server.enqueue(json(LibraryItemsResponse(results = listOf(changed), total = 1)))
    server.enqueue(json(BatchGetResponse(listOf(changed))))

    sync(
      changed,
      contentRepo,
      chapterRepo(before.chapters()),
      bookmarkRepo = mockk { coEvery { bookmarks(any()) } returns emptyList() },
      listed = true,
    )

    assertFalse(cover.exists())
    val stores = written.flatten().filter { it.id == stored.id }
    assertTrue(stores.isNotEmpty() && stores.all { it.metadata.cover == null })
  }

  @Test
  fun `a library of 5,000 books is written in a few steps`() = runTest {
    val written = mutableListOf<List<BookContent>>()
    val contentRepo = emptyContentRepo()
    coEvery { contentRepo.put(capture(written)) } returns Unit

    syncLargeLibrary(contentRepo, items(5_000))

    val books = written.sumOf { it.size }
    val steps = written.count { it.isNotEmpty() }
    assertEquals(5_000, books)
    assertTrue(steps <= 5)
    coVerify(exactly = 0) { contentRepo.put(any<BookContent>()) }
  }

  @Test
  fun `a seek while the sync stores a book keeps its place`() = runTest {
    val before = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")), updatedAt = 1_000)
    val previousChapters = before.chapters()
    var current = before.toBookContent(previousChapters, existing = null)
    val contentRepo =
      mockk<BookContentRepo> {
        coEvery { all() } answers { listOf(current) }
        coEvery { get(current.id) } answers { current }
        coEvery { put(any<List<BookContent>>()) } answers
          {
            firstArg<List<BookContent>>().forEach { current = it }
          }
      }
    val chapterRepo =
      mockk<ChapterRepo> {
        coEvery { get(any<ChapterId>()) } answers
          {
            previousChapters.find { it.id == firstArg() }
          }
        coEvery { put(any<List<Chapter>>()) } answers
          {
            // the listener seeks while the sync writes the chapters
            current = current.copy(positionInChapter = 20_000)
          }
      }
    val changed = before.copy(updatedAt = 2_000)
    server.enqueue(json(LibraryItemsResponse(results = listOf(changed), total = 1)))
    server.enqueue(json(BatchGetResponse(listOf(changed))))

    sync(
      changed,
      contentRepo,
      chapterRepo,
      bookmarkRepo = mockk { coEvery { bookmarks(any()) } returns emptyList() },
      listed = true,
    )

    assertEquals(20_000, current.positionInChapter)
  }

  @Test
  fun `a seek while the sync writes a batch of books keeps its place`() = runTest {
    val before = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")), updatedAt = 1_000)
    val chapters = before.chapters()
    var current = before.toBookContent(chapters, existing = null)
    val added = item(tracks = listOf(track(1, 0.0, 60.0, "b.mp3")), id = "added")
    val contentRepo =
      mockk<BookContentRepo> {
        coEvery { all() } answers { listOf(current) }
        coEvery { this@mockk.get(any()) } answers { current.takeIf { it.id == firstArg() } }
        coEvery { put(any<List<BookContent>>()) } answers
          {
            val written = firstArg<List<BookContent>>()
            // the listener seeks while the sync writes the new books
            if (written.any { it.id.value == "abs://item/added" }) {
              current = current.copy(positionInChapter = 20_000)
            }
            written.filter { it.id == current.id }.forEach { current = it }
          }
      }
    val chapterRepo =
      mockk<ChapterRepo>(relaxUnitFun = true) {
        coEvery { get(any<ChapterId>()) } answers { chapters.find { it.id == firstArg() } }
      }
    val changed = before.copy(updatedAt = 2_000)
    server.enqueue(json(LibraryItemsResponse(results = listOf(changed, added), total = 2)))
    server.enqueue(json(BatchGetResponse(listOf(changed, added))))

    sync(
      changed,
      contentRepo,
      chapterRepo,
      bookmarkRepo = mockk { coEvery { bookmarks(any()) } returns emptyList() },
      listed = true,
    )

    assertEquals(20_000, current.positionInChapter)
  }

  @Test
  fun `books that got through before the connection dropped are kept`() = runTest {
    assertEquals(50, booksKeptAfter(MockResponse.Builder().onResponseStart(CloseSocket()).build()))
  }

  @Test
  fun `books that got through before the server failed are kept`() = runTest {
    assertEquals(50, booksKeptAfter(MockResponse(code = 503)))
  }

  private suspend fun booksKeptAfter(failure: MockResponse): Int {
    val written = mutableListOf<List<BookContent>>()
    val contentRepo = emptyContentRepo()
    coEvery { contentRepo.put(capture(written)) } returns Unit

    val sync = runCatching { syncLargeLibrary(contentRepo, items(120), failure = failure) }

    assertTrue(sync.isFailure)
    return written.sumOf { it.size }
  }

  // writes like the real repository, which reads each book again under its lock
  private fun bookRepository(contentRepo: BookContentRepo): BookRepository {
    val bookRepository = mockk<BookRepository>()
    coEvery { bookRepository.updateBooks(any(), any()) } coAnswers
      {
        val update = secondArg<(BookContent) -> BookContent>()
        contentRepo.put(firstArg<Set<BookId>>().mapNotNull { contentRepo.get(it)?.let(update) })
      }
    return bookRepository
  }

  private fun emptyContentRepo(): BookContentRepo {
    val contentRepo = mockk<BookContentRepo>(relaxUnitFun = true)
    coEvery { contentRepo.all() } returns emptyList()
    coEvery { contentRepo.get(any()) } returns null
    return contentRepo
  }

  private fun items(count: Int) =
    List(count) { item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")), id = "item$it") }

  private suspend fun syncLargeLibrary(
    contentRepo: BookContentRepo,
    items: List<AbsLibraryItem>,
    failure: MockResponse? = null,
  ) {
    val itemsById = items.associateBy { it.id }
    var batches = 0
    server.dispatcher =
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          val path = request.url.encodedPath
          return when {
            path.endsWith("/items") && request.method == "GET" -> {
              val page = request.url.queryParameter("page")!!.toInt()
              val limit = request.url.queryParameter("limit")!!.toInt()
              json(
                LibraryItemsResponse(
                  results = items.drop(page * limit).take(limit),
                  total = items.size,
                ),
              )
            }
            path == "/api/items/batch/get" && failure != null && ++batches > 1 -> failure
            path == "/api/items/batch/get" -> {
              val ids =
                audiobookshelfJson
                  .decodeFromString<BatchGetRequest>(request.body!!.utf8())
                  .libraryItemIds
              json(BatchGetResponse(ids.map(itemsById::getValue)))
            }
            path == "/api/libraries" -> json(LibrariesResponse(emptyList()))
            else -> MockResponse(code = 404)
          }
        }
      }
    LibrarySync(
        http = AudiobookshelfHttp(accountStore, OkHttpClient()),
        contentRepo = contentRepo,
        bookRepository = bookRepository(contentRepo),
        chapterRepo = mockk(relaxUnitFun = true),
        bookmarkRepo = mockk(),
        accountStore = accountStore,
      )
      .sync(accountStore.data.first()!!)
  }

  private fun chapterRepo(chapters: List<Chapter>) =
    mockk<ChapterRepo>(relaxUnitFun = true) {
      coEvery { get(any<ChapterId>()) } answers { chapters.find { it.id == firstArg() } }
    }

  private fun contentRepo(stored: BookContent) =
    mockk<BookContentRepo>(relaxUnitFun = true) {
      coEvery { all() } returns listOf(stored)
      coEvery { get(stored.id) } returns stored
    }

  private suspend fun sync(
    item: AbsLibraryItem,
    contentRepo: BookContentRepo,
    chapterRepo: ChapterRepo,
    bookmarkRepo: BookmarkRepo = mockk(),
    listed: Boolean = false,
  ) {
    if (!listed) server.enqueue(json(LibraryItemsResponse(results = listOf(item), total = 1)))
    server.enqueue(
      json(
        LibrariesResponse(listOf(AbsLibrary(id = "library", name = "Kids", mediaType = "book"))),
      ),
    )
    LibrarySync(
        http = AudiobookshelfHttp(accountStore, OkHttpClient()),
        contentRepo = contentRepo,
        bookRepository = bookRepository(contentRepo),
        chapterRepo = chapterRepo,
        bookmarkRepo = bookmarkRepo,
        accountStore = accountStore,
      )
      .sync(accountStore.data.first()!!)
  }

  private inline fun <reified T> json(body: T) =
    MockResponse(code = 200, body = audiobookshelfJson.encodeToString(body))
}
