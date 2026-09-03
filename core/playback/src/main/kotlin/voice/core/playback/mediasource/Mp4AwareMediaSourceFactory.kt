package voice.core.playback.mediasource

import androidx.media3.common.C
import androidx.media3.common.FileTypes
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.drm.DrmSessionManagerProvider
import androidx.media3.exoplayer.source.ClippingMediaSource
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.upstream.LoadErrorHandlingPolicy

/**
 * Routes mp4 containers to [PlatformMediaExtractor] and everything else to media3's own extractors.
 */
internal class Mp4AwareMediaSourceFactory(
  private val default: MediaSource.Factory,
  private val mp4: MediaSource.Factory,
) : MediaSource.Factory {

  override fun createMediaSource(mediaItem: MediaItem): MediaSource {
    return if (isMp4(mediaItem)) {
      maybeClip(mediaItem, mp4.createMediaSource(mediaItem))
    } else {
      default.createMediaSource(mediaItem)
    }
  }

  // Matches .mp4, .m4a, .m4b and .m4v.
  private fun isMp4(mediaItem: MediaItem): Boolean {
    val uri = mediaItem.localConfiguration?.uri ?: return false
    return FileTypes.inferFileTypeFromUri(uri) == FileTypes.MP4
  }

  private fun maybeClip(
    mediaItem: MediaItem,
    source: MediaSource,
  ): MediaSource {
    val clipping = mediaItem.clippingConfiguration
    if (clipping.startPositionUs == 0L &&
      clipping.endPositionUs == C.TIME_END_OF_SOURCE &&
      !clipping.relativeToDefaultPosition
    ) {
      return source
    }
    return ClippingMediaSource.Builder(source)
      .setStartPositionUs(clipping.startPositionUs)
      .setEndPositionUs(clipping.endPositionUs)
      .setEnableInitialDiscontinuity(!clipping.startsAtKeyFrame)
      .setAllowDynamicClippingUpdates(clipping.relativeToLiveWindow)
      .setRelativeToDefaultPosition(clipping.relativeToDefaultPosition)
      .setAllowUnseekableMedia(clipping.allowUnseekableMedia)
      .build()
  }

  override fun getSupportedTypes(): IntArray = default.supportedTypes

  override fun setDrmSessionManagerProvider(drmSessionManagerProvider: DrmSessionManagerProvider): MediaSource.Factory {
    default.setDrmSessionManagerProvider(drmSessionManagerProvider)
    mp4.setDrmSessionManagerProvider(drmSessionManagerProvider)
    return this
  }

  override fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): MediaSource.Factory {
    default.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
    mp4.setLoadErrorHandlingPolicy(loadErrorHandlingPolicy)
    return this
  }
}
