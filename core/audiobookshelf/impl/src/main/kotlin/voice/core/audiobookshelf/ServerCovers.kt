@file:OptIn(ExperimentalCoilApi::class)

package voice.core.audiobookshelf

import coil3.ImageLoader
import coil3.Uri
import coil3.annotation.ExperimentalCoilApi
import coil3.fetch.FetchResult
import coil3.fetch.Fetcher
import coil3.network.CacheStrategy
import coil3.network.DeDupeConcurrentRequestStrategy
import coil3.network.HttpException
import coil3.network.NetworkRequest
import coil3.network.NetworkResponse
import coil3.network.okhttp.OkHttpNetworkFetcherFactory
import coil3.pathSegments
import coil3.request.Options
import coil3.toUri
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.binding
import java.io.IOException
import kotlinx.coroutines.flow.first
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.data.AUDIOBOOKSHELF_SCHEME

@ContributesIntoSet(AppScope::class, binding = binding<Fetcher.Factory<Uri>>())
@Inject
public class ServerCovers
internal constructor(
  private val http: AudiobookshelfHttp,
  private val accountStore: AccountStore,
) : Fetcher.Factory<Uri> {

  // the car and the notification ask for a new cover at once, and both read it from the cache
  private val dedupe = DeDupeConcurrentRequestStrategy()
  private val network =
    OkHttpNetworkFetcherFactory(
      callFactory = { http.authenticatedClient },
      cacheStrategy = { ImagesOnly },
      concurrentRequestStrategy = { dedupe },
    )

  override fun create(
    data: Uri,
    options: Options,
    imageLoader: ImageLoader,
  ): Fetcher? {
    if (data.scheme != AUDIOBOOKSHELF_SCHEME || data.authority != "item") return null
    val itemId = data.pathSegments.takeIf { it.size == 2 && it[1] == "cover" }?.first()
    if (itemId == null) return null
    // the server address isn't part of the key, so the cache stays when it changes
    val cacheOptions = options.copy(diskCacheKey = options.diskCacheKey ?: data.toString())
    return Fetcher {
      val serverUrl = accountStore.data.first()?.serverUrl ?: throw IOException("Signed out")
      try {
        fetch(
          "${serverUrl}api/items/$itemId/cover?width=800&format=jpeg",
          cacheOptions,
          imageLoader,
        )
      } catch (e: HttpException) {
        // a book without a cover has nothing to fall back to
        if (e.response.code == 404) throw e
        // the server can fail to scale some images, the original still works
        fetch("${serverUrl}api/items/$itemId/cover?raw=1", cacheOptions, imageLoader)
      }
    }
  }

  private suspend fun fetch(
    url: String,
    options: Options,
    imageLoader: ImageLoader,
  ): FetchResult? = network.create(url.toUri(), options, imageLoader)?.fetch()
}

// a page of a captive portal would hide the cover until the cache drops it
private object ImagesOnly : CacheStrategy {
  override suspend fun read(
    cacheResponse: NetworkResponse,
    networkRequest: NetworkRequest,
    options: Options,
  ): CacheStrategy.ReadResult = CacheStrategy.DEFAULT.read(cacheResponse, networkRequest, options)

  override suspend fun write(
    cacheResponse: NetworkResponse?,
    networkRequest: NetworkRequest,
    networkResponse: NetworkResponse,
    options: Options,
  ): CacheStrategy.WriteResult {
    if (networkResponse.headers["Content-Type"]?.startsWith("image/") != true) {
      return CacheStrategy.WriteResult.DISABLED
    }
    return CacheStrategy.DEFAULT.write(cacheResponse, networkRequest, networkResponse, options)
  }
}
