package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.Player
import kotlin.time.Duration
import kotlin.time.Duration.Companion.ZERO
import kotlin.time.Duration.Companion.milliseconds

internal fun Player.seekBackBy(
  rewind: Duration,
  crossMediaItems: Boolean,
) {
  var currentPosition = currentPosition.takeUnless { it == C.TIME_UNSET }?.milliseconds?.coerceAtLeast(ZERO) ?: return
  var remaining = rewind
  var mediaItemIndex = currentMediaItemIndex.takeUnless { it == C.INDEX_UNSET } ?: return

  while (remaining > currentPosition) {
    if (!crossMediaItems) {
      seekTo(mediaItemIndex, 0)
      return
    }
    remaining -= currentPosition
    val previousMediaItemIndex = mediaItemIndex - 1
    if (previousMediaItemIndex < 0) {
      seekTo(0, 0)
      return
    }
    currentPosition = getMediaItemAt(previousMediaItemIndex).mediaMetadata.durationMs?.milliseconds ?: return
    mediaItemIndex = previousMediaItemIndex
  }

  seekTo(mediaItemIndex, (currentPosition - remaining).inWholeMilliseconds)
}
