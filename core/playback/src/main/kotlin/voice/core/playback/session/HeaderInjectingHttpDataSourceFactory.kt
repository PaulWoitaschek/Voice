package voice.core.playback.session

import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.datasource.HttpDataSource
import voice.core.data.playback.RemoteAuthHeaderProvider

@UnstableApi
internal class HeaderInjectingHttpDataSourceFactory(
  private val headerProviders: Set<@JvmSuppressWildcards RemoteAuthHeaderProvider>,
) : DataSource.Factory {

  private val delegateFactory = DefaultHttpDataSource.Factory()

  override fun createDataSource(): DataSource {
    return HeaderInjectingDataSource(delegateFactory.createDataSource(), headerProviders)
  }
}

@UnstableApi
private class HeaderInjectingDataSource(
  private val delegate: HttpDataSource,
  private val headerProviders: Set<RemoteAuthHeaderProvider>,
) : DataSource by delegate {

  override fun open(dataSpec: DataSpec): Long {
    val headers = headerProviders.firstNotNullOfOrNull { it.headersFor(dataSpec.uri) }
    val spec = if (headers.isNullOrEmpty()) dataSpec else dataSpec.withAdditionalHeaders(headers)
    return delegate.open(spec)
  }
}
