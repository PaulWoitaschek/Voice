package voice.core.audiobookshelf.download

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.disk.DiskCache
import java.io.File
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import mockwebserver3.Dispatcher
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import mockwebserver3.RecordedRequest
import okhttp3.OkHttpClient
import okio.Buffer
import okio.ByteString.Companion.decodeBase64
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode
import voice.core.audiobookshelf.MemoryBookRepository
import voice.core.audiobookshelf.MemoryDataStore
import voice.core.audiobookshelf.ServerCovers
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.sync.chapters
import voice.core.audiobookshelf.sync.item
import voice.core.audiobookshelf.sync.toBookContent
import voice.core.audiobookshelf.sync.track
import voice.core.data.Book
import voice.core.data.CustomMetadata

// decoding tells a cover from another answer of the server
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(DelicateCoilApi::class)
@RunWith(AndroidJUnit4::class)
class DownloadedCoversTest {

  private val server = MockWebServer()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val cacheDir = createTempDirectory().toFile()
  private val coverFolder = File(context.filesDir, "bookCovers").apply { mkdirs() }
  private val image =
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
      .decodeBase64()!!

  @Before
  fun setUp() {
    server.start()
    val accountStore =
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
    SingletonImageLoader.setUnsafe(
      ImageLoader.Builder(context)
        .components {
          add(ServerCovers(AudiobookshelfHttp(accountStore, OkHttpClient()), accountStore))
        }
        .diskCache { DiskCache.Builder().directory(cacheDir.toOkioPath()).build() }
        .build(),
    )
  }

  @After
  fun tearDown() {
    SingletonImageLoader.reset()
    server.close()
    cacheDir.deleteRecursively()
    coverFolder.deleteRecursively()
  }

  @Test
  fun `a server book that isn't downloaded keeps no cover on the phone`() = runTest {
    val cover = File(coverFolder, "abs-item-1000.jpg").apply { writeText("cover") }
    val book = book(cover)
    val repo = MemoryBookRepository(book)

    DownloadedCovers(repo, context).sync(listOf(book), downloaded = emptySet())

    assertFalse(cover.exists())
    assertNull(repo.cover(book))
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `a cover a listener picked for a server book stays`() = runTest {
    val picked = File(coverFolder, "picked.jpg").apply { writeText("picked") }
    val streamed = book(picked)
    val downloaded = book(picked, itemId = "downloaded")
    val repo = MemoryBookRepository(streamed, downloaded)

    DownloadedCovers(repo, context)
      .sync(listOf(streamed, downloaded), downloaded = setOf(downloaded.id))

    assertEquals("picked", picked.readText())
    assertEquals(picked, repo.cover(streamed))
    assertEquals(picked, repo.cover(downloaded))
    assertEquals(0, server.requestCount)
  }

  @Test
  fun `a downloaded server book keeps its cover on the phone`() = runTest {
    server.enqueue(image())
    val book = book(cover = null)
    val repo = MemoryBookRepository(book)

    DownloadedCovers(repo, context).keep(book)

    val cover = repo.cover(book)!!
    assertEquals(File(coverFolder, "abs-item-2000.jpg"), cover)
    assertContentEquals(image.toByteArray(), cover.readBytes())
  }

  @Test
  fun `a seek while the cover loads keeps its place`() = runTest {
    val book = book(cover = null)
    val repo = MemoryBookRepository(book)
    server.dispatcher =
      object : Dispatcher() {
        override fun dispatch(request: RecordedRequest): MockResponse {
          runBlocking { repo.updateBook(book.id) { it.copy(positionInChapter = 20_000) } }
          return image()
        }
      }

    DownloadedCovers(repo, context).keep(book)

    assertEquals(20_000, repo.get(book.id)!!.content.positionInChapter)
    assertEquals(File(coverFolder, "abs-item-2000.jpg"), repo.cover(book))
  }

  @Test
  fun `a downloaded book whose cover file is gone gets it again`() = runTest {
    server.enqueue(image())
    val cover = File(coverFolder, "abs-item-2000.jpg")
    val book = book(cover)

    DownloadedCovers(MemoryBookRepository(book), context)
      .sync(listOf(book), downloaded = setOf(book.id))

    assertTrue(cover.exists())
  }

  @Test
  fun `a downloaded book changed on the server gets the new cover`() = runTest {
    server.enqueue(image())
    val old = File(coverFolder, "abs-item-1000.jpg").apply { writeText("old") }
    val book = book(old)
    val repo = MemoryBookRepository(book)

    DownloadedCovers(repo, context).sync(listOf(book), downloaded = setOf(book.id))

    assertFalse(old.exists())
    assertEquals(File(coverFolder, "abs-item-2000.jpg"), repo.cover(book))
  }

  @Test
  fun `an answer of the server that isn't an image doesn't become a cover`() = runTest {
    server.enqueue(MockResponse(code = 200, body = "<html>Sign in</html>"))
    val book = book(cover = null)
    val repo = MemoryBookRepository(book)

    DownloadedCovers(repo, context).keep(book)

    assertFalse(File(coverFolder, "abs-item-2000.jpg").exists())
    assertNull(repo.cover(book))
  }

  @Test
  fun `a downloaded server book with its cover on the phone doesn't load it again`() = runTest {
    val cover = File(coverFolder, "abs-item-2000.jpg").apply { writeText("cover") }
    val book = book(cover)
    val repo = MemoryBookRepository(book)

    DownloadedCovers(repo, context).sync(listOf(book), downloaded = setOf(book.id))

    assertTrue(cover.exists())
    assertEquals(cover, repo.cover(book))
    assertEquals(0, server.requestCount)
  }

  private fun image() =
    MockResponse.Builder()
      .code(200)
      .addHeader("Content-Type", "image/png")
      .body(Buffer().write(image))
      .build()

  private fun book(
    cover: File?,
    itemId: String = "item",
  ): Book {
    val item = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")), id = itemId, updatedAt = 2_000)
    val chapters = item.chapters()
    val content = item.toBookContent(chapters, existing = null)
    val withCover = content.copy(metadata = content.metadata.copy(cover = cover))
    return Book(withCover, chapters, CustomMetadata(withCover.id))
  }

  private suspend fun MemoryBookRepository.cover(book: Book): File? =
    get(book.id)!!.content.metadata.cover
}
