package voice.core.playback.session

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import coil3.ImageLoader
import coil3.SingletonImageLoader
import coil3.annotation.DelicateCoilApi
import coil3.disk.DiskCache
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.OutputStream
import kotlin.io.path.createTempDirectory
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okio.Buffer
import okio.ByteString.Companion.decodeBase64
import okio.Path.Companion.toOkioPath
import org.junit.After
import org.junit.Before
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

// decoding tells a cover from another answer of the server
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@OptIn(DelicateCoilApi::class)
@RunWith(AndroidJUnit4::class)
class CoverProviderTest {

  private val server = MockWebServer()
  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val cacheDir = createTempDirectory().toFile()
  private val image =
    "iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mP8z8BQDwAEhQGAhKmMIQAAAABJRU5ErkJggg=="
      .decodeBase64()!!

  @Before
  fun setUp() {
    server.start()
    SingletonImageLoader.setUnsafe(
      ImageLoader.Builder(context)
        .diskCache { DiskCache.Builder().directory(cacheDir.toOkioPath()).build() }
        .build(),
    )
  }

  @After
  fun tearDown() {
    SingletonImageLoader.reset()
    server.close()
    cacheDir.deleteRecursively()
  }

  @Test
  fun `a cover that isn't cached loads, and then comes from the cache`() = runTest {
    server.enqueue(MockResponse.Builder().code(200).body(Buffer().write(image)).build())
    val cover = server.url("/cover").toString()

    assertContentEquals(image.toByteArray(), written(cover))
    server.close()

    assertContentEquals(image.toByteArray(), written(cover))
    assertEquals(1, server.requestCount)
  }

  @Test
  fun `a cover that can't load gives no cover`() = runTest {
    server.enqueue(MockResponse(code = 404))

    assertContentEquals(ByteArray(0), written(server.url("/cover").toString()))
  }

  @Test
  fun `an answer that isn't an image gives no cover`() = runTest {
    server.enqueue(MockResponse(code = 200, body = "<html>Sign in</html>"))

    assertContentEquals(ByteArray(0), written(server.url("/cover").toString()))
  }

  @Test
  fun `an app that stops reading the cover doesn't break Voice`() = runTest {
    server.enqueue(MockResponse.Builder().code(200).body(Buffer().write(image)).build())
    val closed =
      object : OutputStream() {
        override fun write(b: Int) = throw IOException("Broken pipe")
      }

    writeCover(context, server.url("/cover").toString(), closed)
  }

  private suspend fun written(cover: String): ByteArray {
    val output = ByteArrayOutputStream()
    writeCover(context, cover, output)
    return output.toByteArray()
  }
}
