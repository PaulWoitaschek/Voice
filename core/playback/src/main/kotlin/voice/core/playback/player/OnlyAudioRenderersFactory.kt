package voice.core.playback.player

import android.content.Context
import android.os.Handler
import android.os.Looper
import androidx.media3.common.audio.SonicAudioProcessor
import androidx.media3.exoplayer.DefaultRenderersFactory
import androidx.media3.exoplayer.Renderer
import androidx.media3.exoplayer.audio.AudioSink
import androidx.media3.exoplayer.audio.DefaultAudioSink
import androidx.media3.exoplayer.audio.SilenceSkippingAudioProcessor
import androidx.media3.exoplayer.mediacodec.MediaCodecSelector
import androidx.media3.exoplayer.metadata.MetadataOutput
import androidx.media3.exoplayer.text.TextOutput
import androidx.media3.exoplayer.video.VideoRendererEventListener
import dev.zacsweers.metro.Inject

@Inject
class OnlyAudioRenderersFactory(
  context: Context,
  private val silenceSkippingAudioProcessor: SilenceSkippingAudioProcessor,
) : DefaultRenderersFactory(context) {

  override fun buildAudioSink(
    context: Context,
    enableFloatOutput: Boolean,
    enableAudioOutputPlaybackParams: Boolean,
  ): AudioSink {
    val audioProcessorChain = DefaultAudioSink.DefaultAudioProcessorChain(
      emptyArray(),
      silenceSkippingAudioProcessor,
      SonicAudioProcessor(),
    )
    return DefaultAudioSink.Builder(context)
      .setEnableFloatOutput(enableFloatOutput)
      .setEnableAudioOutputPlaybackParameters(enableAudioOutputPlaybackParams)
      .setAudioProcessorChain(audioProcessorChain)
      .build()
  }

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

internal fun audiobookSilenceSkippingAudioProcessor(): SilenceSkippingAudioProcessor {
  return SilenceSkippingAudioProcessor(
    MINIMUM_SILENCE_DURATION_US,
    SILENCE_RETENTION_RATIO,
    MAX_SILENCE_TO_KEEP_DURATION_US,
    MIN_VOLUME_TO_KEEP_PERCENTAGE_WHEN_MUTING,
    SILENCE_THRESHOLD_LEVEL,
  )
}

internal const val MINIMUM_SILENCE_DURATION_US = 1_000_000L
internal const val SILENCE_RETENTION_RATIO = 0.1f
internal const val MAX_SILENCE_TO_KEEP_DURATION_US = 500_000L
internal const val MIN_VOLUME_TO_KEEP_PERCENTAGE_WHEN_MUTING = 10
internal const val SILENCE_THRESHOLD_LEVEL: Short = 2_048
