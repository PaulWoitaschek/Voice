package voice.core.audiobookshelf

import coil3.ImageLoader
import coil3.Uri
import coil3.fetch.Fetcher
import coil3.fetch.SourceFetchResult
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
import voice.core.data.AUDIOBOOKSHELF_SCHEME

@ContributesIntoSet(AppScope::class, binding = binding<Fetcher.Factory<Uri>>())
@Inject
public class ServerCovers internal constructor(private val accountStore: AccountStore) :
  Fetcher.Factory<Uri> {

  override fun create(
    data: Uri,
    options: Options,
    imageLoader: ImageLoader,
  ): Fetcher? {
    if (data.scheme != AUDIOBOOKSHELF_SCHEME || data.authority != "item") return null
    val itemId = data.pathSegments.firstOrNull() ?: return null
    // the server address isn't part of the key, so the cache stays when it changes
    val cacheOptions = options.copy(diskCacheKey = options.diskCacheKey ?: data.toString())
    return Fetcher {
      val serverUrl = accountStore.data.first()?.serverUrl ?: throw IOException("Signed out")
      val url = "${serverUrl}api/items/$itemId/cover?width=800&format=jpeg".toUri()
      val result = imageLoader.components.newFetcher(url, cacheOptions, imageLoader)?.first?.fetch()
      // a captive portal answers with a page, which must not stay in the cache as the cover
      if (result is SourceFetchResult && result.mimeType?.startsWith("image/") != true) {
        result.source.close()
        imageLoader.diskCache?.remove(cacheOptions.diskCacheKey!!)
        throw IOException("The server sent no image")
      }
      result
    }
  }
}
