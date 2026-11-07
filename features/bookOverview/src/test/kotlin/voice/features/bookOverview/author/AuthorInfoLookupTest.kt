package voice.features.bookOverview.author

import java.util.Locale
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import voice.core.common.DispatcherProvider

class AuthorInfoLookupTest {

  @Test
  fun `an author comes with the summary of their page`() = runTest {
    val lookup = lookup { url ->
      when {
        url.isSummary("en", "Stephen King") -> summary(item = "Q39829")
        url.isAuthorSearch("Q39829") -> AUTHOR_FOUND
        else -> null
      }
    }

    assertEquals(
      expected =
        AuthorInfo(
          description = "American author (born 1947)",
          extract = "Stephen Edwin King is an American author.",
          photo = "https://upload.wikimedia.org/king.jpg",
          url = "https://en.wikipedia.org/wiki/Stephen_King",
        ),
      actual = lookup.info("Stephen King", Locale.ENGLISH),
    )
  }

  @Test
  fun `a namesake who wrote nothing is left out`() = runTest {
    val lookup = lookup { url ->
      when {
        url.isSummary("en", "John Williams") -> summary(item = "Q131285")
        url.isAuthorSearch("Q131285") -> NO_AUTHOR_FOUND
        else -> null
      }
    }

    assertEquals(expected = null, actual = lookup.info("John Williams", Locale.ENGLISH))
  }

  @Test
  fun `a page that lists several people is left out`() = runTest {
    val lookup = lookup { url ->
      when {
        url.isSummary("en", "Robert Galbraith") ->
          summary(item = "Q7344652", type = "disambiguation")
        url.isAuthorSearch("Q7344652") -> AUTHOR_FOUND
        else -> null
      }
    }

    assertEquals(expected = null, actual = lookup.info("Robert Galbraith", Locale.ENGLISH))
  }

  @Test
  fun `without a page in the language of the device, the English one is used`() = runTest {
    val lookup = lookup { url ->
      when {
        url.isSummary("en", "Stephen King") -> summary(item = "Q39829")
        url.isAuthorSearch("Q39829") -> AUTHOR_FOUND
        else -> null
      }
    }

    assertEquals(
      expected = "Stephen Edwin King is an American author.",
      actual = lookup.info("Stephen King", Locale.GERMAN)?.extract,
    )
  }

  @Test
  fun `Norwegian uses the Wikipedia under no`() {
    assertEquals(expected = "no", actual = wikipediaLanguage(Locale.forLanguageTag("nb-NO")))
    assertEquals(expected = "he", actual = wikipediaLanguage(Locale.forLanguageTag("he")))
  }

  private fun lookup(respond: (HttpUrl) -> String?): AuthorInfoLookup {
    val client =
      OkHttpClient.Builder()
        .addInterceptor { chain ->
          val body = respond(chain.request().url)
          Response.Builder()
            .request(chain.request())
            .protocol(Protocol.HTTP_1_1)
            .code(if (body == null) 404 else 200)
            .message("")
            .body((body ?: "{}").toResponseBody())
            .build()
        }
        .build()
    val dispatcher = UnconfinedTestDispatcher()
    return AuthorInfoLookup(client, DispatcherProvider(dispatcher, dispatcher, dispatcher))
  }

  private fun HttpUrl.isSummary(
    language: String,
    title: String,
  ): Boolean {
    return host == "$language.wikipedia.org" &&
      pathSegments == listOf("api", "rest_v1", "page", "summary", title)
  }

  private fun HttpUrl.isAuthorSearch(item: String): Boolean {
    return host == "www.wikidata.org" && queryParameter("srsearch") == "haswbstatement:P50=$item"
  }

  private fun summary(
    item: String,
    type: String = "standard",
  ): String {
    return """
      {
        "type": "$type",
        "description": "American author (born 1947)",
        "extract": "Stephen Edwin King is an American author.",
        "thumbnail": { "source": "https://upload.wikimedia.org/king.jpg", "width": 330 },
        "wikibase_item": "$item",
        "content_urls": {
          "desktop": { "page": "https://en.wikipedia.org/wiki/Stephen_King" },
          "mobile": { "page": "https://en.wikipedia.org/wiki/Stephen_King" }
        }
      }
    """
      .trimIndent()
  }
}

private const val AUTHOR_FOUND =
  """{"batchcomplete":"","query":{"search":[{"ns":0,"title":"Q753290","pageid":708707}]}}"""

private const val NO_AUTHOR_FOUND =
  """{"batchcomplete":"","query":{"searchinfo":{"totalhits":0},"search":[]}}"""
