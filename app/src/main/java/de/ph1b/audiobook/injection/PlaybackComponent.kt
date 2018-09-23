package de.ph1b.audiobook.injection

import dagger.BindsInstance
import dagger.Subcomponent
import de.ph1b.audiobook.playback.PlaybackService

@Subcomponent(modules = [PlaybackModule::class])
@PerService
interface PlaybackComponent {

  fun inject(target: PlaybackService)

  @Subcomponent.Builder
  interface Builder {

    @BindsInstance
    fun playbackService(playbackService: PlaybackService): Builder

    fun build(): PlaybackComponent
  }
}
