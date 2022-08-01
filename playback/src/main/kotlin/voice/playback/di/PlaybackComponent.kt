package voice.playback.di

import com.squareup.anvil.annotations.MergeSubcomponent
import dagger.BindsInstance
import dagger.Subcomponent
import voice.playback.session.PlaybackService

@PlaybackScope
@MergeSubcomponent(
  scope = PlaybackScope::class,
)
interface PlaybackComponent {

  fun inject(target: PlaybackService)

  @Subcomponent.Factory
  interface Factory {

    fun create(@BindsInstance playbackService: PlaybackService): PlaybackComponent
  }
}
