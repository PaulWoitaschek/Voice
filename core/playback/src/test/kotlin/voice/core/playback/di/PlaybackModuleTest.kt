package voice.core.playback.di

import androidx.media3.common.TrackSelectionParameters
import io.kotest.matchers.shouldBe
import org.junit.Test

class PlaybackModuleTest {

  @Test
  fun `audio offload preferences enable offload when the feature flag is on`() {
    audioOffloadPreferences(true).audioOffloadMode shouldBe
      TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_ENABLED
  }

  @Test
  fun `audio offload preferences disable offload when the feature flag is off`() {
    audioOffloadPreferences(false).audioOffloadMode shouldBe
      TrackSelectionParameters.AudioOffloadPreferences.AUDIO_OFFLOAD_MODE_DISABLED
  }
}
