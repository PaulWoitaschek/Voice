package voice.core.audiobookshelf.sync

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.encodeToString
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import voice.core.audiobookshelf.MemoryDataStore
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.api.AbsLibrary
import voice.core.audiobookshelf.api.AbsLibraryItem
import voice.core.audiobookshelf.api.LibrariesResponse
import voice.core.audiobookshelf.api.LibraryItemsResponse
import voice.core.audiobookshelf.audiobookshelfJson
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.BookContent
import voice.core.data.ChapterId
import voice.core.data.repo.BookContentRepo
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

    coVerify { contentRepo.put(stored.copy(serverLibraryId = "library")) }
    assertEquals(mapOf("library" to "Kids"), accountStore.data.first()?.libraryNames)
  }

  @Test
  fun `a book the server flags gets its library and stays as it is`() = runTest {
    val item = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")))
    val stored = item.toBookContent(item.chapters(), existing = null).copy(serverLibraryId = null)
    val contentRepo = contentRepo(stored)

    sync(item.copy(isMissing = true), contentRepo, chapterRepo = mockk())

    coVerify { contentRepo.put(stored.copy(serverLibraryId = "library")) }
  }

  private fun contentRepo(stored: BookContent) =
    mockk<BookContentRepo> {
      coEvery { all() } returns listOf(stored)
      coEvery { get(stored.id) } returns stored
      coEvery { put(any()) } just Runs
    }

  private suspend fun sync(
    item: AbsLibraryItem,
    contentRepo: BookContentRepo,
    chapterRepo: ChapterRepo,
  ) {
    server.enqueue(json(LibraryItemsResponse(results = listOf(item), total = 1)))
    server.enqueue(
      json(
        LibrariesResponse(listOf(AbsLibrary(id = "library", name = "Kids", mediaType = "book"))),
      ),
    )
    LibrarySync(
        http = AudiobookshelfHttp(accountStore, OkHttpClient()),
        contentRepo = contentRepo,
        chapterRepo = chapterRepo,
        bookmarkRepo = mockk(),
        accountStore = accountStore,
        context = mockk(),
      )
      .sync(accountStore.data.first()!!)
  }

  private inline fun <reified T> json(body: T) =
    MockResponse(code = 200, body = audiobookshelfJson.encodeToString(body))
}
