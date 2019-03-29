package de.ph1b.audiobook.playback

import android.content.Context
import android.os.Handler
import android.os.Looper
import com.google.android.exoplayer2.DefaultRenderersFactory
import com.google.android.exoplayer2.Renderer
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.FrameworkMediaCrypto
import com.google.android.exoplayer2.mediacodec.MediaCodecSelector
import com.google.android.exoplayer2.text.TextOutput
import com.google.android.exoplayer2.video.VideoRendererEventListener
import java.util.ArrayList
import javax.inject.Inject

class OnlyAudioRenderersFactory @Inject constructor(
  context: Context
) : DefaultRenderersFactory(context) {

  override fun buildVideoRenderers(
    context: Context?,
    extensionRendererMode: Int,
    mediaCodecSelector: MediaCodecSelector?,
    drmSessionManager: DrmSessionManager<FrameworkMediaCrypto>?,
    playClearSamplesWithoutKeys: Boolean,
    eventHandler: Handler?,
    eventListener: VideoRendererEventListener?,
    allowedVideoJoiningTimeMs: Long,
    out: ArrayList<Renderer>?
  ) {
  }

  override fun buildTextRenderers(
    context: Context?,
    output: TextOutput?,
    outputLooper: Looper?,
    extensionRendererMode: Int,
    out: ArrayList<Renderer>?
  ) {
  }
}
