package voice.core.audiobookshelf.sync

import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import voice.core.audiobookshelf.MemoryBookRepository
import voice.core.audiobookshelf.MemoryDataStore
import voice.core.audiobookshelf.TestAppInfo
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.SyncedProgress
import voice.core.audiobookshelf.api.AbsMediaProgress
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager
import java.time.Instant
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class ProgressSyncTest {

  private val server = MockWebServer()
  private lateinit var account: Account

  private val item = item(
    tracks = listOf(track(1, 0.0, 60.0, "a.mp3"), track(2, 60.0, 60.0, "b.mp3")),
    id = "item",
  )
  private val chapters = item.chapters()
  private val bookId = BookId("abs://item/item")
  private val syncedProgress = MemoryDataStore<Map<String, SyncedProgress>>(emptyMap())

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
  fun `progress from another device is taken over`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 0))

    progressSync(repo).syncAll(account, listOf(serverProgress(currentTime = 80.0, lastUpdate = 10)))

    assertEquals(80_000, repo.get(bookId)!!.position)
    assertEquals(chapters[1].id, repo.get(bookId)!!.content.currentChapter)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `progress played here is sent`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000))
    syncedProgress.updateData { mapOf(bookId.value to SyncedProgress(0, finished = false, serverLastUpdate = 10)) }
    server.enqueue(MockResponse(code = 200))

    progressSync(repo).syncAll(account, listOf(serverProgress(currentTime = 0.0, lastUpdate = 10)))

    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("PATCH", request.method)
    assertEquals("/api/me/progress/item", request.url.encodedPath)
    val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
    assertEquals(30.0, body.getValue("currentTime").jsonPrimitive.double)
    assertEquals(false, body.getValue("isFinished").jsonPrimitive.boolean)
    assertEquals(30_000, syncedProgress.data.first()[bookId.value]!!.positionMs)
  }

  @Test
  fun `a book in its last seconds stays there when the server marked it finished`() = runTest {
    // the server counts the last ten seconds as finished and resets a finished book when it's told otherwise
    val repo = MemoryBookRepository(book(positionMs = 112_000))
    syncedProgress.updateData { mapOf(bookId.value to SyncedProgress(112_000, finished = true, serverLastUpdate = 10)) }

    progressSync(repo).syncAll(
      account,
      listOf(serverProgress(currentTime = 112.0, lastUpdate = 20, isFinished = true)),
    )

    assertEquals(112_000, repo.get(bookId)!!.position)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `a book finished on another device moves to its end`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000))
    syncedProgress.updateData { mapOf(bookId.value to SyncedProgress(30_000, finished = false, serverLastUpdate = 10)) }

    progressSync(repo).syncAll(
      account,
      listOf(serverProgress(currentTime = 50.0, lastUpdate = 20, isFinished = true)),
    )

    assertEquals(120_000, repo.get(bookId)!!.position)
  }

  @Test
  fun `when both sides moved, the newer position wins`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000, lastPlayedAt = 5_000))
    syncedProgress.updateData { mapOf(bookId.value to SyncedProgress(0, finished = false, serverLastUpdate = 1)) }

    progressSync(repo).syncAll(account, listOf(serverProgress(currentTime = 90.0, lastUpdate = 9_000)))

    assertEquals(90_000, repo.get(bookId)!!.position)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `what Voice sent itself doesn't count as a change on the server`() = runTest {
    // marked as finished here, without playing, after the earlier position went up
    val repo = MemoryBookRepository(book(positionMs = 120_000, lastPlayedAt = 1))
    syncedProgress.updateData { mapOf(bookId.value to SyncedProgress(30_000, finished = false, serverLastUpdate = 10)) }
    server.enqueue(MockResponse(code = 200))

    progressSync(repo).syncAll(account, listOf(serverProgress(currentTime = 30.0, lastUpdate = 20)))

    assertEquals(120_000, repo.get(bookId)!!.position)
    val body = Json.parseToJsonElement(server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8()).jsonObject
    assertEquals(true, body.getValue("isFinished").jsonPrimitive.boolean)
  }

  @Test
  fun `a position that couldn't be sent goes up with the next sync`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000))
    val sync = progressSync(repo)
    server.enqueue(MockResponse(code = 500))
    server.enqueue(MockResponse(code = 200))

    assertEquals(false, sync.push(account, bookId))
    assertNull(syncedProgress.data.first()[bookId.value])

    assertEquals(true, sync.push(account, bookId))
    assertEquals(2, server.requestCount)
    assertEquals(30_000, syncedProgress.data.first()[bookId.value]!!.positionMs)
  }

  @Test
  fun `a position that didn't move isn't sent again`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000))
    syncedProgress.updateData { mapOf(bookId.value to SyncedProgress(30_500, finished = false, serverLastUpdate = 10)) }

    assertEquals(true, progressSync(repo).push(account, bookId))
    assertEquals(0, server.requestCount)
  }

  private fun progressSync(repo: MemoryBookRepository): ProgressSync {
    val accountStore = MemoryDataStore<Account?>(account)
    return ProgressSync(
      http = AudiobookshelfHttp(accountStore, TestAppInfo),
      bookRepository = repo,
      syncedProgressStore = syncedProgress,
      currentBookStore = MemoryDataStore<BookId?>(null),
      playStateManager = PlayStateManager(),
      playerController = mockk {
        coEvery { livePlaybackState(any()) } returns null
      },
    )
  }

  private fun book(
    positionMs: Long,
    lastPlayedAt: Long = 0,
  ): Book {
    val content = item.toBookContent(chapters, existing = null).copy(lastPlayedAt = Instant.ofEpochMilli(lastPlayedAt))
    return Book(content, chapters).let { book ->
      val position = book.positionAt(positionMs)
      book.update { it.copy(currentChapter = position.chapterId, positionInChapter = position.positionInChapter) }
    }
  }

  private fun serverProgress(
    currentTime: Double,
    lastUpdate: Long,
    isFinished: Boolean = false,
  ) = AbsMediaProgress(
    libraryItemId = "item",
    duration = 120.0,
    currentTime = currentTime,
    isFinished = isFinished,
    lastUpdate = lastUpdate,
  )
}
