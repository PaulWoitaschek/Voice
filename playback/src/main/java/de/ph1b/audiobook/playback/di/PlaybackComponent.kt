package de.ph1b.audiobook.playback.di

import dagger.BindsInstance
import dagger.Subcomponent
import de.ph1b.audiobook.playback.session.PlaybackService

@Subcomponent(modules = [PlaybackServiceModule::class])
@PerService
interface PlaybackComponent {

  fun inject(target: PlaybackService)

  @Subcomponent.Factory
  interface Factory {

    fun create(@BindsInstance playbackService: PlaybackService): PlaybackComponent
  }
}
