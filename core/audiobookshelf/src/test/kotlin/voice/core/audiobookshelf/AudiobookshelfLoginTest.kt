package voice.core.audiobookshelf

import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import okhttp3.Headers
import org.junit.After
import org.junit.Before
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.download.AudiobookshelfDownloads
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs

class AudiobookshelfLoginTest {

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
  fun `a server is found and signed in to`() = runTest {
    server.enqueue(
      MockResponse(code = 200, body = """{"app":"audiobookshelf","serverVersion":"2.37.1","isInit":true,"authMethods":["local"]}"""),
    )
    server.enqueue(MockResponse(code = 200, body = """{"user":{"id":"1","username":"paul","accessToken":"a","refreshToken":"r"}}"""))
    val audiobookshelf = audiobookshelf()

    val found = assertIs<FindServerResult.Found>(audiobookshelf.findServer(server.url("/").toString()))
    val login = audiobookshelf.login(found.server, "paul", "voice")

    assertIs<LoginResult.Success>(login)
    val requests = List(2) { server.takeRequest() }
    assertEquals("true", requests.last().headers["x-return-tokens"])
  }

  @Test
  fun `a page in front of the server doesn't break the login`() = runTest {
    server.enqueue(
      MockResponse(code = 200, body = """{"app":"audiobookshelf","serverVersion":"2.37.1","isInit":true,"authMethods":["local"]}"""),
    )
    server.enqueue(MockResponse(code = 200, body = "<html>Sign in to the proxy</html>"))
    val audiobookshelf = audiobookshelf()

    val found = assertIs<FindServerResult.Found>(audiobookshelf.findServer(server.url("/").toString()))

    assertEquals(LoginResult.Failed, audiobookshelf.login(found.server, "paul", "voice"))
  }

  @Test
  fun `a server that is too old is turned down`() = runTest {
    server.enqueue(
      MockResponse(code = 200, body = """{"app":"audiobookshelf","serverVersion":"2.20.0","isInit":true,"authMethods":["local"]}"""),
    )

    assertEquals(FindServerResult.TooOld("2.20.0"), audiobookshelf().findServer(server.url("/").toString()))
  }

  @Test
  fun `a server that redirects is stored where the redirect ends`() = runTest {
    server.enqueue(MockResponse(code = 308, headers = Headers.headersOf("Location", "/audiobookshelf/status")))
    server.enqueue(
      MockResponse(code = 200, body = """{"app":"audiobookshelf","serverVersion":"2.37.1","isInit":true,"authMethods":["local"]}"""),
    )

    val found = assertIs<FindServerResult.Found>(audiobookshelf().findServer(server.url("/").toString()))

    assertEquals(server.url("/audiobookshelf/").toString(), found.server.url)
  }

  @Test
  fun `signing in again with another account keeps the one that is there`() = runTest {
    server.enqueue(
      MockResponse(code = 200, body = """{"app":"audiobookshelf","serverVersion":"2.37.1","isInit":true,"authMethods":["local"]}"""),
    )
    server.enqueue(MockResponse(code = 200, body = """{"user":{"id":"2","username":"kim","accessToken":"a","refreshToken":"r"}}"""))
    val stored = Account(
      serverUrl = server.url("/").toString(),
      userId = "1",
      username = "paul",
      accessToken = "old",
      refreshToken = "old",
      libraryIds = listOf("library"),
      serverVersion = "2.37.1",
    )
    val accountStore = MemoryDataStore<Account?>(stored)
    val audiobookshelf = audiobookshelf(accountStore)

    val found = assertIs<FindServerResult.Found>(audiobookshelf.findServer(server.url("/").toString()))
    val login = assertIs<LoginResult.Success>(audiobookshelf.login(found.server, "kim", "voice"))

    assertFalse(audiobookshelf.renewLogin(login.login))
    assertEquals(stored, accountStore.data.first())
  }

  @Test
  fun `connecting another account signs out the one that is there`() = runTest {
    server.enqueue(
      MockResponse(code = 200, body = """{"app":"audiobookshelf","serverVersion":"2.37.1","isInit":true,"authMethods":["local"]}"""),
    )
    server.enqueue(MockResponse(code = 200, body = """{"user":{"id":"2","username":"kim","accessToken":"a","refreshToken":"r"}}"""))
    // the logout of the previous account
    server.enqueue(MockResponse(code = 200))
    val accountStore = MemoryDataStore<Account?>(
      Account(
        serverUrl = server.url("/").toString(),
        userId = "1",
        username = "paul",
        accessToken = "old",
        refreshToken = "old",
        libraryIds = listOf("library"),
        serverVersion = "2.37.1",
      ),
    )
    val downloads = mockk<AudiobookshelfDownloads>(relaxed = true)
    val audiobookshelf = audiobookshelf(accountStore, downloads)

    val found = assertIs<FindServerResult.Found>(audiobookshelf.findServer(server.url("/").toString()))
    val login = assertIs<LoginResult.Success>(audiobookshelf.login(found.server, "kim", "voice"))
    audiobookshelf.connect(login.login, listOf("library"))

    coVerify { downloads.removeAll() }
    assertEquals("2", accountStore.data.first()?.userId)
  }

  private fun audiobookshelf(
    accountStore: MemoryDataStore<Account?> = MemoryDataStore(null),
    downloads: AudiobookshelfDownloads = mockk(relaxed = true),
  ): Audiobookshelf {
    return Audiobookshelf(
      accountStore = accountStore,
      http = AudiobookshelfHttp(accountStore, TestAppInfo),
      sync = mockk(relaxed = true),
      progressSync = mockk(relaxed = true),
      downloads = downloads,
      contentRepo = mockk(relaxed = true),
      currentBookStore = MemoryDataStore(null),
      playerController = mockk(relaxed = true),
      context = mockk(relaxed = true),
      analytics = mockk(relaxed = true),
    )
  }
}
