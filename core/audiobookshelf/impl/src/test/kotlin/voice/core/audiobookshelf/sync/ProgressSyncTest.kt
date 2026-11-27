package voice.core.audiobookshelf.sync

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.spyk
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.boolean
import kotlinx.serialization.json.double
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import voice.core.audiobookshelf.MemoryBookRepository
import voice.core.audiobookshelf.MemoryDataStore
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.account.SyncedProgress
import voice.core.audiobookshelf.account.SyncedProgressStore
import voice.core.audiobookshelf.api.AbsMediaProgress
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.CustomMetadata
import voice.core.data.repo.BookRepository
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.playstate.PlayStateManager

class ProgressSyncTest {

  private val server = MockWebServer()
  private lateinit var account: Account

  private val item =
    item(
      tracks = listOf(track(1, 0.0, 60.0, "a.mp3"), track(2, 60.0, 60.0, "b.mp3")),
      id = "item",
    )
  private val chapters = item.chapters()
  private val bookId = BookId("abs://item/item")
  private val syncedProgress = MemoryDataStore<Map<String, SyncedProgress>>(emptyMap())

  @Before
  fun setUp() {
    server.start()
    account =
      Account(
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
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(0, finished = false, serverLastUpdate = 10))
    }
    server.enqueue(MockResponse(code = 200))

    progressSync(repo).syncAll(account, listOf(serverProgress(currentTime = 0.0, lastUpdate = 10)))

    val request = server.takeRequest(1, TimeUnit.SECONDS)!!
    assertEquals("PATCH", request.method)
    assertEquals("/api/me/progress/item", request.url.encodedPath)
    val body = Json.parseToJsonElement(request.body!!.utf8()).jsonObject
    assertEquals(30.0, body.getValue("currentTime").jsonPrimitive.double)
    assertNull(body["isFinished"])
    assertEquals(30_000, syncedProgress.data.first()[bookId.value]!!.positionMs)
  }

  @Test
  fun `a book that plays sends the position of the player`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000))
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(0, finished = false, serverLastUpdate = 10))
    }
    server.enqueue(MockResponse(code = 200))

    progressSync(repo, player(positionMs = 40_000, isPlaying = true))
      .syncAll(account, listOf(serverProgress(currentTime = 0.0, lastUpdate = 10)))

    val body =
      Json.parseToJsonElement(server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8()).jsonObject
    assertEquals(40.0, body.getValue("currentTime").jsonPrimitive.double)
    assertEquals(40_000, syncedProgress.data.first()[bookId.value]!!.positionMs)
  }

  @Test
  fun `a paused book sends its stored position`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000))
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(0, finished = false, serverLastUpdate = 10))
    }
    server.enqueue(MockResponse(code = 200))

    progressSync(repo, player(positionMs = 40_000, isPlaying = false))
      .syncAll(account, listOf(serverProgress(currentTime = 0.0, lastUpdate = 10)))

    val body =
      Json.parseToJsonElement(server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8()).jsonObject
    assertEquals(30.0, body.getValue("currentTime").jsonPrimitive.double)
    assertEquals(30_000, syncedProgress.data.first()[bookId.value]!!.positionMs)
  }

  @Test
  fun `listening to a finished book again doesn't send it back to its start`() = runTest {
    // told that a finished book isn't finished, the server starts it over. Without the flag it
    // keeps the time.
    val repo = MemoryBookRepository(book(positionMs = 30_000))
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(120_000, finished = true, serverLastUpdate = 10))
    }
    server.enqueue(MockResponse(code = 200))

    progressSync(repo)
      .syncAll(
        account,
        listOf(serverProgress(currentTime = 120.0, lastUpdate = 10, isFinished = true)),
      )

    val body =
      Json.parseToJsonElement(server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8()).jsonObject
    assertEquals(30.0, body.getValue("currentTime").jsonPrimitive.double)
    assertNull(body["isFinished"])
  }

  @Test
  fun `a finished book marked as not started starts over on the server too`() = runTest {
    // the server keeps a finished book finished when only its time is sent and the time didn't
    // change
    val repo = MemoryBookRepository(book(positionMs = 0))
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(120_000, finished = true, serverLastUpdate = 10))
    }
    server.enqueue(MockResponse(code = 200))

    progressSync(repo)
      .syncAll(
        account,
        listOf(serverProgress(currentTime = 0.0, lastUpdate = 10, isFinished = true)),
      )

    val body =
      Json.parseToJsonElement(server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8()).jsonObject
    assertEquals(false, body.getValue("isFinished").jsonPrimitive.boolean)
  }

  @Test
  fun `a book in its last seconds stays there when the server marked it finished`() = runTest {
    // the server counts the last ten seconds as finished and resets a finished book when it's told
    // otherwise
    val repo = MemoryBookRepository(book(positionMs = 112_000))
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(112_000, finished = true, serverLastUpdate = 10))
    }

    progressSync(repo)
      .syncAll(
        account,
        listOf(serverProgress(currentTime = 112.0, lastUpdate = 20, isFinished = true)),
      )

    assertEquals(112_000, repo.get(bookId)!!.position)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `a book finished on another device moves to its end`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000))
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(30_000, finished = false, serverLastUpdate = 10))
    }

    progressSync(repo)
      .syncAll(
        account,
        listOf(serverProgress(currentTime = 50.0, lastUpdate = 20, isFinished = true)),
      )

    assertEquals(120_000, repo.get(bookId)!!.position)
  }

  @Test
  fun `when both sides moved, the newer position wins`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 30_000, lastPlayedAt = 5_000))
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(0, finished = false, serverLastUpdate = 1))
    }

    progressSync(repo)
      .syncAll(account, listOf(serverProgress(currentTime = 90.0, lastUpdate = 9_000)))

    assertEquals(90_000, repo.get(bookId)!!.position)
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `what Voice sent itself doesn't count as a change on the server`() = runTest {
    // marked as finished here, without playing, after the earlier position went up
    val repo = MemoryBookRepository(book(positionMs = 120_000, lastPlayedAt = 1))
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(30_000, finished = false, serverLastUpdate = 10))
    }
    server.enqueue(MockResponse(code = 200))

    progressSync(repo).syncAll(account, listOf(serverProgress(currentTime = 30.0, lastUpdate = 20)))

    assertEquals(120_000, repo.get(bookId)!!.position)
    val body =
      Json.parseToJsonElement(server.takeRequest(1, TimeUnit.SECONDS)!!.body!!.utf8()).jsonObject
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
    syncedProgress.updateData {
      mapOf(bookId.value to SyncedProgress(30_500, finished = false, serverLastUpdate = 10))
    }

    assertEquals(true, progressSync(repo).push(account, bookId))
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `the places of 1,000 books from another app are taken over in one write`() = runTest {
    val books = List(1_000) { book(positionMs = 0, itemId = "item$it") }
    val repo = spyk(MemoryBookRepository(*books.toTypedArray()))
    val progress =
      List(1_000) {
        serverProgress(currentTime = 80.0, lastUpdate = 10, itemId = "item$it")
      }

    progressSync(repo).syncAll(account, progress)

    assertTrue(repo.all().all { it.position == 80_000L })
    assertEquals(1_000, syncedProgress.data.first().size)
    coVerify(exactly = 1) { repo.updateBooks(any(), any()) }
    coVerify(exactly = 0) { repo.updateBook(any(), any()) }
  }

  @Test
  fun `a seek during the sync keeps its place`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 0))
    val playerController =
      mockk<PlayerController> {
        coEvery { livePlaybackState(any()) } coAnswers
          {
            // the listener seeks while the sync decides
            repo.updateBook(bookId) { it.copy(positionInChapter = 20_000) }
            null
          }
      }

    progressSync(repo, playerController)
      .syncAll(account, listOf(serverProgress(currentTime = 80.0, lastUpdate = 10)))

    assertEquals(20_000, repo.get(bookId)!!.position)
  }

  @Test
  fun `a play at the same place during the sync keeps its place`() = runTest {
    val repo = MemoryBookRepository(book(positionMs = 0))
    val playerController =
      mockk<PlayerController> {
        coEvery { livePlaybackState(any()) } coAnswers
          {
            // the listener starts the book while the sync decides
            repo.updateBook(bookId) { it.copy(lastPlayedAt = Instant.fromEpochMilliseconds(5)) }
            null
          }
      }

    progressSync(repo, playerController)
      .syncAll(account, listOf(serverProgress(currentTime = 80.0, lastUpdate = 10)))

    assertEquals(0, repo.get(bookId)!!.position)
  }

  private fun player(
    positionMs: Long,
    isPlaying: Boolean,
  ): PlayerController = mockk {
    coEvery { livePlaybackState(bookId) } returns
      LivePlaybackState(
        bookId = bookId,
        chapterId = chapters[0].id,
        positionMs = positionMs,
        isPlaying = isPlaying,
        playbackSpeed = 1F,
      )
  }

  private fun progressSync(
    repo: BookRepository,
    playerController: PlayerController = mockk {
      coEvery { livePlaybackState(any()) } returns null
    },
  ): ProgressSync {
    val accountStore = MemoryDataStore<Account?>(account)
    return ProgressSync(
      http =
        AudiobookshelfHttp(
          AccountStore(accountStore),
          OkHttpClient(),
        ),
      bookRepository = repo,
      syncedProgressStore = SyncedProgressStore(syncedProgress),
      playStateManager = PlayStateManager(),
      playerController = playerController,
    )
  }

  private fun book(
    positionMs: Long,
    lastPlayedAt: Long = 0,
    itemId: String = "item",
  ): Book {
    val serverItem = item.copy(id = itemId)
    val chapters = serverItem.chapters()
    val content =
      serverItem
        .toBookContent(chapters, existing = null)
        .copy(lastPlayedAt = Instant.fromEpochMilliseconds(lastPlayedAt))
    return Book(content, chapters, CustomMetadata(content.id)).let { book ->
      val position = book.positionAt(positionMs)
      book.update {
        it.copy(currentChapter = position.chapterId, positionInChapter = position.positionInChapter)
      }
    }
  }

  private fun serverProgress(
    currentTime: Double,
    lastUpdate: Long,
    isFinished: Boolean = false,
    itemId: String = "item",
  ) =
    AbsMediaProgress(
      libraryItemId = itemId,
      duration = 120.0,
      currentTime = currentTime,
      isFinished = isFinished,
      lastUpdate = lastUpdate,
    )
}
