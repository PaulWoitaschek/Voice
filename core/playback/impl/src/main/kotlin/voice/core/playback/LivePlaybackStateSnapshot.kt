package voice.core.playback

import androidx.media3.common.C
import androidx.media3.common.util.Util
import androidx.media3.session.MediaController
import voice.core.data.BookId
import voice.core.playback.session.bookId
import voice.core.playback.session.positionInChapter
import voice.core.playback.session.realChapterId
import voice.core.playback.session.toMediaIdOrNull

internal fun MediaController.livePlaybackStateSnapshot(bookId: BookId? = null): LivePlaybackState? {
  val mediaId = currentMediaItem?.mediaId?.toMediaIdOrNull() ?: return null
  val mediaItemBookId = mediaId.bookId ?: return null
  if (bookId != null && mediaItemBookId != bookId) {
    return null
  }
  val positionMs = currentPosition.takeUnless { it == C.TIME_UNSET || it < 0 } ?: return null
  val chapterId = mediaId.realChapterId ?: return null
  val positionInChapter = mediaId.positionInChapter(positionMs) ?: return null
  return LivePlaybackState(
    bookId = mediaItemBookId,
    chapterId = chapterId,
    positionMs = positionInChapter,
    isPlaying = !Util.shouldShowPlayButton(this),
    playbackSpeed = playbackParameters.speed,
  )
}
