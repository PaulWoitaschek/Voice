package voice.core.playback.di

import dev.zacsweers.metro.Qualifier

/**
 * The data source for media that isn't on the device, like books on a server. It also handles plain http.
 */
@Qualifier
annotation class RemoteMediaDataSource
