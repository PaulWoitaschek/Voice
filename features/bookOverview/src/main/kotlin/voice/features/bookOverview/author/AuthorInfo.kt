package voice.features.bookOverview.author

import android.content.Context
import androidx.compose.runtime.Immutable
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import okhttp3.Cache
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.coroutines.executeAsync
import voice.core.common.DispatcherProvider

@Immutable
data class AuthorInfo(
  val description: String?,
  val extract: String,
  val photo: AuthorPhoto?,
  val url: String,
)

@Immutable
data class AuthorPhoto(
  val url: String,
  val page: String,
)

// A name alone often leads to someone else, like the composer for John Williams, so a page only
// counts when Wikidata knows its person as the author of a work
class AuthorInfoLookup(
  private val client: OkHttpClient,
  private val dispatcherProvider: DispatcherProvider,
) {

  private val json = Json { ignoreUnknownKeys = true }

  suspend fun info(
    author: String,
    locale: Locale,
  ): AuthorInfo? {
    val summary =
      listOf(wikipediaLanguage(locale), "en").distinct().firstNotNullOfOrNull { language ->
        summary(language, author)
      } ?: return null
    val item = summary.wikibaseItem ?: return null
    val extract = summary.extract?.trim()?.takeIf { it.isNotEmpty() } ?: return null
    if (summary.type != "standard" || !isAuthor(item)) return null
    return AuthorInfo(
      description = summary.description?.replaceFirstChar { it.titlecase(locale) },
      extract = extract,
      photo = summary.thumbnail?.source?.let(::commonsPhoto),
      url = summary.contentUrls.mobile.page,
    )
  }

  private suspend fun summary(
    language: String,
    title: String,
  ): Summary? {
    val url =
      HttpUrl.Builder()
        .scheme("https")
        .host("$language.wikipedia.org")
        .addPathSegments("api/rest_v1/page/summary")
        .addPathSegment(title)
        .build()
    return get(url)?.let { json.decodeFromString<Summary>(it) }
  }

  private suspend fun isAuthor(item: String): Boolean {
    val url =
      "https://www.wikidata.org/w/api.php"
        .toHttpUrl()
        .newBuilder()
        .addQueryParameter("action", "query")
        .addQueryParameter("format", "json")
        .addQueryParameter("list", "search")
        .addQueryParameter("srsearch", "haswbstatement:P50=$item")
        .addQueryParameter("srlimit", "1")
        .addQueryParameter("srprop", "")
        .build()
    val body = get(url) ?: return false
    return json.decodeFromString<WikidataSearch>(body).query.search.isNotEmpty()
  }

  private suspend fun get(url: HttpUrl): String? {
    return client.newCall(Request(url)).executeAsync().use { response ->
      when {
        response.code == 404 -> null
        !response.isSuccessful -> throw IOException("HTTP ${response.code}")
        else -> withContext(dispatcherProvider.io) { response.body.string() }
      }
    }
  }

  // Wikipedia also shows images that it may only use as fair use, which doesn't extend to apps
  private fun commonsPhoto(url: String): AuthorPhoto? {
    val segments = url.toHttpUrlOrNull()?.pathSegments ?: return null
    if (segments.take(2) != listOf("wikipedia", "commons")) return null
    val file = segments.getOrNull(if (segments.getOrNull(2) == "thumb") 5 else 4) ?: return null
    val page =
      HttpUrl.Builder()
        .scheme("https")
        .host("commons.wikimedia.org")
        .addPathSegment("wiki")
        .addPathSegment("File:$file")
        .build()
    return AuthorPhoto(url = url, page = page.toString())
  }

  @Serializable
  private data class Summary(
    val type: String,
    val description: String? = null,
    val extract: String? = null,
    val thumbnail: Thumbnail? = null,
    @SerialName("wikibase_item") val wikibaseItem: String? = null,
    @SerialName("content_urls") val contentUrls: ContentUrls,
  )

  @Serializable private data class Thumbnail(val source: String)

  @Serializable private data class ContentUrls(val mobile: PageUrls)

  @Serializable private data class PageUrls(val page: String)

  @Serializable private data class WikidataSearch(val query: Query)

  @Serializable private data class Query(val search: List<JsonObject>)
}

internal fun wikipediaLanguage(locale: Locale): String {
  val language = locale.toLanguageTag().substringBefore('-')
  // Norwegian Bokmål has its Wikipedia under no
  return if (language == "nb") "no" else language
}

@BindingContainer
@ContributesTo(AppScope::class)
object AuthorInfoModule {

  @Provides
  @SingleIn(AppScope::class)
  fun authorInfoLookup(
    okHttpClient: OkHttpClient,
    context: Context,
    dispatcherProvider: DispatcherProvider,
  ): AuthorInfoLookup {
    val client =
      okHttpClient
        .newBuilder()
        .cache(Cache(File(context.cacheDir, "authorInfo"), maxSize = 5L * 1024 * 1024))
        // Wikipedia allows 5 minutes and Wikidata none, but what we show about an author rarely
        // changes. Missing pages are kept as well, so authors without one cost no requests.
        .addNetworkInterceptor { chain ->
          chain
            .proceed(chain.request())
            .newBuilder()
            .header("Cache-Control", "max-age=${30.days.inWholeSeconds}")
            .build()
        }
        .build()
    return AuthorInfoLookup(client, dispatcherProvider)
  }
}
