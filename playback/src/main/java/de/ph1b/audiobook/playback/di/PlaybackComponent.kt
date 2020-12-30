package de.ph1b.audiobook.playback.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Subcomponent
import de.ph1b.audiobook.playback.session.PlaybackService

@PlaybackScope
@MergeSubcomponent(
  scope = PlaybackScope::class
)
interface PlaybackComponent {

  fun inject(target: PlaybackService)

  @Subcomponent.Factory
  interface Factory {

    fun create(@BindsInstance playbackService: PlaybackService): PlaybackComponent
  }
}
