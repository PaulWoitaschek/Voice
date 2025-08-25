package voice.core.playback.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import voice.core.playback.session.PlaybackService

@GraphExtension(
  scope = PlaybackScope::class,
)
@PlaybackScope
interface PlaybackGraph {

  fun inject(target: PlaybackService)

  @ContributesTo(AppScope::class)
  @GraphExtension.Factory
  interface Factory {
    fun create(@Provides playbackService: PlaybackService): PlaybackGraph
  }

  @ContributesTo(AppScope::class)
  interface Provider {
    val playbackGraphFactory: Factory
  }
}
