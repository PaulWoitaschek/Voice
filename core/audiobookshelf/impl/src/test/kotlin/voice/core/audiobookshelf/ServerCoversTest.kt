package voice.core.audiobookshelf

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.decode.DataSource
import coil3.disk.DiskCache
import coil3.fetch.SourceFetchResult
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.request.Options
import coil3.toUri
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers.Companion.headersOf
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore

@RunWith(AndroidJUnit4::class)
class ServerCoversTest {

  private val server = MockWebServer()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val cacheDir = createTempDirectory().toFile()
  private lateinit var covers: ServerCovers
  private lateinit var imageLoader: ImageLoader

  @Before
  fun setUp() {
    server.start()
    val account =
      Account(
        serverUrl = server.url("/").toString(),
        userId = "user",
        username = "paul",
        accessToken = "access",
        refreshToken = "refresh",
        libraryIds = listOf("library"),
        serverVersion = "2.37.1",
      )
    covers = ServerCovers(AccountStore(MemoryDataStore<Account?>(account)))
    imageLoader =
      ImageLoader.Builder(context)
        .components {
          add(covers)
          add(OkHttpNetworkFetcherFactory())
        }
        .diskCache { DiskCache.Builder().directory(cacheDir.toOkioPath()).build() }
        .build()
  }

  @After
  fun tearDown() {
    server.close()
    cacheDir.deleteRecursively()
  }

  @Test
  fun `the cover of a server book loads from the server when it shows`() = runTest {
    server.enqueue(cover())

    assertEquals(DataSource.NETWORK, fetch().dataSource)
    val request = server.takeRequest()
    assertEquals("/api/items/item/cover", request.url.encodedPath)
    assertEquals("width=800&format=jpeg", request.url.encodedQuery)
  }

  @Test
  fun `a cover seen before loads from the cache offline`() = runTest {
    server.enqueue(cover())
    assertEquals(DataSource.NETWORK, fetch().dataSource)
    server.close()

    assertEquals(DataSource.DISK, fetch().dataSource)
  }

  @Test
  fun `a change of the book on the server loads its cover again`() = runTest {
    server.enqueue(cover())
    server.enqueue(cover())
    assertEquals(DataSource.NETWORK, fetch(updatedAt = 2_000).dataSource)

    assertEquals(DataSource.NETWORK, fetch(updatedAt = 3_000).dataSource)
    assertEquals(2, server.requestCount)
  }

  private fun cover() =
    MockResponse(code = 200, headers = headersOf("Content-Type", "image/jpeg"), body = "cover")

  private suspend fun fetch(updatedAt: Long = 2_000): SourceFetchResult {
    val fetcher =
      covers.create("abs://item/item/cover?ts=$updatedAt".toUri(), Options(context), imageLoader)!!
    // the image loader fetches on a background thread
    val result = withContext(Dispatchers.IO) { fetcher.fetch() }
    return (result as SourceFetchResult).also { it.source.close() }
  }
}
