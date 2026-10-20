package voice.features.cover.api

import kotlinx.coroutines.test.runTest
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import kotlin.test.Test
import kotlin.test.assertEquals

// Responses recorded from DuckDuckGo. Calling it from CI fails whenever it blocks the runner.
private const val SEARCH_PAGE = """
<script>DDG.deep.initialize('/d.js?q=unicorns',false);</script>
<script>var cfg={locale:"us-en",vqd="4-271763288492266336876506996475182048150",safe_ddg=0};</script>
<link rel="preload" href="https://links.duckduckgo.com/d.js?q=unicorns&t=D&l=us-en&vqd=4-271763288492266336876506996475182048150&bing_market=en-US" as="script">
"""

private const val IMAGE_RESULTS = """
{
  "next": "i.js?q=unicorns&p=1&s=100&u=bing&l=us-en",
  "query": "unicorns",
  "response_type": "images",
  "results": [
    {
      "height": 1280,
      "image": "https://cdn.pixabay.com/photo/2024/03/11/18/00/ai-generated-8627371_1280.jpg",
      "source": "Bing",
      "thumbnail": "https://tse4.mm.bing.net/th/id/OIP.HhPsXKpgW0Pg2ujkKe75eAHaHa?r=0&pid=Api",
      "title": "Unicorn LED String of Lights",
      "width": 1280
    }
  ],
  "vqd": { "unicorns": "4-271763288492266336876506996475182048150" }
}
"""

internal class CoverApiTest {

  private val client = OkHttpClient.Builder()
    .addInterceptor { chain ->
      val request = chain.request()
      val (body, type) = when (request.url.encodedPath) {
        "/" -> SEARCH_PAGE to "text/html"
        "/i.js" -> IMAGE_RESULTS to "application/json"
        else -> error("Unexpected request to ${request.url.encodedPath}")
      }
      Response.Builder()
        .request(request)
        .protocol(Protocol.HTTP_1_1)
        .code(200)
        .message("OK")
        .body(body.toResponseBody(type.toMediaType()))
        .build()
    }
    .build()

  private val api = CoverApi(CoverModule.internalApi(client))

  @Test
  fun `reads the token from the search page`() = runTest {
    assertEquals("4-271763288492266336876506996475182048150", api.token("unicorns"))
  }

  @Test
  fun `parses the image results`() = runTest {
    val response = api.search(query = "unicorns", auth = "4-271763288492266336876506996475182048150")

    assertEquals("i.js?q=unicorns&p=1&s=100&u=bing&l=us-en", response.next)
    assertEquals(
      listOf(
        SearchResponse.ImageResult(
          width = 1280,
          height = 1280,
          image = "https://cdn.pixabay.com/photo/2024/03/11/18/00/ai-generated-8627371_1280.jpg",
          thumbnail = "https://tse4.mm.bing.net/th/id/OIP.HhPsXKpgW0Pg2ujkKe75eAHaHa?r=0&pid=Api",
        ),
      ),
      response.results,
    )
  }
}
