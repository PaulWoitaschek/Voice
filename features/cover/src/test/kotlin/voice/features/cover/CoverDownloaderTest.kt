package voice.features.cover

import android.content.Context
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Call
import okhttp3.EventListener
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class CoverDownloaderTest {

  private val server = MockWebServer()

  @Before
  fun setUp() {
    server.start()
  }

  @After
  fun tearDown() {
    server.close()
  }

  @Test
  fun `a failed download is closed off the main thread`() = runTest {
    server.enqueue(MockResponse(code = 403, body = "forbidden"))
    var closedOnMainThread: Boolean? = null
    val client =
      OkHttpClient.Builder()
        .eventListener(
          object : EventListener() {
            override fun callEnd(call: Call) {
              closedOnMainThread = Looper.getMainLooper().isCurrentThread
            }
          },
        )
        .build()
    val downloader = CoverDownloader(client, ApplicationProvider.getApplicationContext<Context>())

    val file = downloader.download(server.url("/cover.jpg").toString())

    assertNull(file)
    assertEquals(false, closedOnMainThread)
  }
}
