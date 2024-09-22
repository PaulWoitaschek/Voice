package voice.playback.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import javax.inject.Inject

class OnlyAudioRenderersFactory
@Inject constructor(context: Context) : DefaultRenderersFactory(context) {

  override fun buildVideoRenderers(
    context: Context,
    extensionRendererMode: Int,
    mediaCodecSelector: MediaCodecSelector,
    enableDecoderFallback: Boolean,
    eventHandler: Handler,
    eventListener: VideoRendererEventListener,
    allowedVideoJoiningTimeMs: Long,
    out: ArrayList<Renderer>,
  ) {
  }

  override fun buildTextRenderers(
    context: Context,
    output: TextOutput,
    outputLooper: Looper,
    extensionRendererMode: Int,
    out: ArrayList<Renderer>,
  ) {
  }

  override fun buildMetadataRenderers(
    context: Context,
    output: MetadataOutput,
    outputLooper: Looper,
    extensionRendererMode: Int,
    out: ArrayList<Renderer>,
  ) {
  }

  override fun buildCameraMotionRenderers(
    context: Context,
    extensionRendererMode: Int,
    out: ArrayList<Renderer>,
  ) {
  }

  override fun buildMiscellaneousRenderers(
    context: Context,
    eventHandler: Handler,
    extensionRendererMode: Int,
    out: ArrayList<Renderer>,
  ) {
  }
}
