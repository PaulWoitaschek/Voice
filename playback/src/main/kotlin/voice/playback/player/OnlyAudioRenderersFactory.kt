package voice.playback.player

import android.content.Context
import android.os.Handler
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.RenderersFactory
import androidx.media3.exoplayer.audio.AudioCapabilities
import androidx.media3.exoplayer.audio.AudioRendererEventListener
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink.OFFLOAD_MODE_ENABLED_GAPLESS_DISABLED
import androidx.media3.exoplayer.audio.MediaCodecAudioRenderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import javax.inject.Inject

class OnlyAudioRenderersFactory
@Inject constructor(
  private val context: Context,
) : RenderersFactory {

  override fun createRenderers(
    eventHandler: Handler,
    videoRendererEventListener: VideoRendererEventListener,
    audioRendererEventListener: AudioRendererEventListener,
    textRendererOutput: TextOutput,
    metadataRendererOutput: MetadataOutput,
  ): Array<Renderer> {
    val sink = DefaultAudioSink.Builder()
      .setAudioCapabilities(AudioCapabilities.getCapabilities(context))
      .setEnableAudioTrackPlaybackParams(true)
      .setOffloadMode(OFFLOAD_MODE_ENABLED_GAPLESS_DISABLED)
      .build()
    return arrayOf(
      MediaCodecAudioRenderer(
        context,
        MediaCodecSelector.DEFAULT,
        eventHandler,
        audioRendererEventListener,
        sink,
      ),
    )
  }
}
