package voice.core.audiobookshelf

import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import mockwebserver3.MockResponse
import mockwebserver3.MockWebServer
import org.junit.After
import org.junit.Before
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import kotlin.test.Test
import kotlin.test.assertEquals
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

  private fun audiobookshelf(): Audiobookshelf {
    val accountStore = MemoryDataStore<Account?>(null)
    return Audiobookshelf(
      accountStore = accountStore,
      http = AudiobookshelfHttp(accountStore, TestAppInfo),
      sync = mockk(relaxed = true),
      progressSync = mockk(relaxed = true),
      downloads = mockk(relaxed = true),
      contentRepo = mockk(relaxed = true),
      currentBookStore = MemoryDataStore(null),
      playerController = mockk(relaxed = true),
      context = mockk(relaxed = true),
      analytics = mockk(relaxed = true),
    )
  }
}
