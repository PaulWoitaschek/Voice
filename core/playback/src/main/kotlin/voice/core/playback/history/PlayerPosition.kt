package voice.core.playback.history

import androidx.media3.common.C
import androidx.media3.common.Player
import voice.core.playback.session.bookId
import voice.core.playback.session.positionInChapter
import voice.core.playback.session.realChapterId
import voice.core.playback.session.toMediaIdOrNull

internal fun Player.playbackPosition(): PlaybackPosition? {
  val positionMs = currentPosition.takeUnless { it == C.TIME_UNSET || it < 0 } ?: return null
  return playbackPosition(currentMediaItemIndex, positionMs)
}

internal fun Player.playbackPosition(
  mediaItemIndex: Int,
  positionInMediaItemMs: Long,
): PlaybackPosition? {
  if (mediaItemIndex == C.INDEX_UNSET || mediaItemIndex !in 0 until mediaItemCount) return null
  val mediaId = getMediaItemAt(mediaItemIndex).mediaId.toMediaIdOrNull() ?: return null
  return PlaybackPosition(
    bookId = mediaId.bookId ?: return null,
    chapterId = mediaId.realChapterId ?: return null,
    time = mediaId.positionInChapter(positionInMediaItemMs.coerceAtLeast(0)) ?: return null,
  )
}
