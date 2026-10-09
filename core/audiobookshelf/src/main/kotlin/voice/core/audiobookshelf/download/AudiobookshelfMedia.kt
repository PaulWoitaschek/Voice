package voice.core.audiobookshelf.download

import android.content.Context
import androidx.core.net.toUri
import androidx.datastore.core.DataStore
import androidx.media3.database.StandaloneDatabaseProvider
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.ResolvingDataSource
import androidx.media3.datasource.cache.CacheDataSource
import androidx.media3.datasource.cache.NoOpCacheEvictor
import androidx.media3.datasource.cache.SimpleCache
import androidx.media3.datasource.okhttp.OkHttpDataSource
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.trackRef
import voice.core.playback.di.RemoteMediaDataSource
import java.io.File
import java.io.IOException

/**
 * The audio of server books. Their chapters point to `abs://` ids, which only turn into urls on the server when
 * the audio is fetched. Downloads live in a cache keyed by those ids, so a downloaded book plays from the device
 * no matter where the server is.
 */
@SingleIn(AppScope::class)
@Inject
internal class AudiobookshelfMedia(
  context: Context,
  http: AudiobookshelfHttp,
  @AccountStore
  private val accountStore: DataStore<Account?>,
) {

  val databaseProvider = StandaloneDatabaseProvider(context)

  // no backup: downloads are large and come back from the server
  val cache = SimpleCache(
    File(context.noBackupFilesDir, "audiobookshelf/downloads"),
    NoOpCacheEvictor(),
    databaseProvider,
  )

  private val upstream: DataSource.Factory = ResolvingDataSource.Factory(
    OkHttpDataSource.Factory(http.authenticatedClient),
  ) { dataSpec ->
    val track = trackRef(dataSpec.uri.toString()) ?: return@Factory dataSpec
    val account = runBlocking { accountStore.data.first() }
      ?: throw IOException("Not signed in to Audiobookshelf")
    dataSpec.withUri("${account.serverUrl}api/items/${track.itemId}/file/${track.ino}".toUri())
  }

  /**
   * Plays what was downloaded and streams the rest. Streaming doesn't write to the cache, which only holds what
   * the listener chose to download.
   */
  val playbackDataSourceFactory: DataSource.Factory = CacheDataSource.Factory()
    .setCache(cache)
    .setUpstreamDataSourceFactory(upstream)
    .setCacheWriteDataSinkFactory(null)
    .setFlags(CacheDataSource.FLAG_IGNORE_CACHE_ON_ERROR)

  val downloadDataSourceFactory: DataSource.Factory = CacheDataSource.Factory()
    .setCache(cache)
    .setUpstreamDataSourceFactory(upstream)
}

@BindingContainer
@ContributesTo(AppScope::class)
object AudiobookshelfMediaModule {

  @Provides
  @RemoteMediaDataSource
  private fun remoteMediaDataSourceFactory(media: AudiobookshelfMedia): DataSource.Factory = media.playbackDataSourceFactory
}
