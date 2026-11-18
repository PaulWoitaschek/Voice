package voice.core.audiobookshelf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.fetch.FetchResult
import coil3.fetch.SourceFetchResult
import coil3.network.HttpException
import coil3.request.Options
import coil3.toUri
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers.Companion.headersOf
import okhttp3.OkHttpClient
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.http.AudiobookshelfHttp

@RunWith(AndroidJUnit4::class)
class ServerCoversTest {

  private val server = MockWebServer()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val cacheDir = createTempDirectory().toFile()
  private val imageLoader =
    ImageLoader.Builder(context)
      .diskCache { DiskCache.Builder().directory(cacheDir.toOkioPath()).build() }
      .build()
  private lateinit var accountStore: AccountStore
  private lateinit var covers: ServerCovers

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
    covers = ServerCovers(AudiobookshelfHttp(accountStore, OkHttpClient()), accountStore)
  }

  @After
  fun tearDown() {
    server.close()
    cacheDir.deleteRecursively()
  }

  @Test
  fun `the cover of a server book loads from the server when it shows`() = runTest {
    server.enqueue(cover())

    val result = fetch()

    assertEquals(DataSource.NETWORK, result.dataSource)
    val request = server.takeRequest()
    assertEquals("/api/items/item/cover", request.url.encodedPath)
    assertEquals("Bearer access", request.headers["Authorization"])
  }

  @Test
  fun `a cover seen before loads from the cache offline`() = runTest {
    server.enqueue(cover())
    assertEquals(DataSource.NETWORK, fetch().dataSource)
    server.close()

    assertEquals(DataSource.DISK, fetch().dataSource)
  }

  @Test
  fun `a cover never seen doesn't load offline`() = runTest {
    server.close()

    assertFailsWith<IOException> { fetch() }
  }

  @Test
  fun `a cover the server can't scale loads in its original size`() = runTest {
    server.enqueue(MockResponse(code = 500))
    server.enqueue(cover())

    assertEquals(DataSource.NETWORK, fetch().dataSource)
    assertEquals("width=800&format=jpeg", server.takeRequest().url.encodedQuery)
    assertEquals("raw=1", server.takeRequest().url.encodedQuery)
  }

  @Test
  fun `a book without a cover costs one request`() = runTest {
    server.enqueue(MockResponse(code = 404))

    assertFailsWith<HttpException> { fetch() }
    assertEquals(1, server.requestCount)
  }

  @Test
  fun `a change of the book on the server loads its cover again`() = runTest {
    server.enqueue(cover())
    server.enqueue(cover())
    assertEquals(DataSource.NETWORK, fetch(updatedAt = 2_000).dataSource)

    assertEquals(DataSource.NETWORK, fetch(updatedAt = 3_000).dataSource)
    assertEquals(2, server.requestCount)
  }

  @Test
  fun `a page that isn't a cover isn't kept`() = runTest {
    server.enqueue(MockResponse(code = 200, body = "<html>Sign in</html>"))

    assertEquals(DataSource.NETWORK, fetch().dataSource)
    assertNull(imageLoader.diskCache!!.openSnapshot("abs://item/item/cover?ts=2000"))
  }

  @Test
  fun `two loads of the same cover at once ask the server once`() = runTest {
    server.enqueue(cover().newBuilder().headersDelay(300, TimeUnit.MILLISECONDS).build())
    server.enqueue(cover())

    val sources = coroutineScope { List(2) { async { fetch().dataSource } }.awaitAll() }

    assertEquals(1, server.requestCount)
    assertEquals(setOf(DataSource.NETWORK, DataSource.DISK), sources.toSet())
  }

  private fun cover() =
    MockResponse(code = 200, headers = headersOf("Content-Type", "image/jpeg"), body = "cover")

  private suspend fun fetch(updatedAt: Long = 2_000): SourceFetchResult {
    val fetcher =
      covers.create("abs://item/item/cover?ts=$updatedAt".toUri(), Options(context), imageLoader)!!
    // the image loader fetches on a background thread
    val result: FetchResult? = withContext(Dispatchers.IO) { fetcher.fetch() }
    return (result as SourceFetchResult).also { it.source.close() }
  }
}
