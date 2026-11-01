package voice.core.playback

import kotlin.time.Duration
import kotlinx.coroutines.flow.Flow
import voice.core.data.BookId
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.playback.misc.Decibel

interface PlayerController {

  /**
   * @param type how the jump is recorded in the listening history: [ListeningEvent.Type.Seek], [ListeningEvent.Type.ChapterChange],
   *   [ListeningEvent.Type.BookmarkJump] or [ListeningEvent.Type.JumpBack].
   */
  fun setPosition(
    time: Long,
    id: ChapterId,
    type: ListeningEvent.Type = ListeningEvent.Type.Seek,
    source: ListeningEvent.Source = ListeningEvent.Source.App,
  )

  fun pauseIfCurrentBookDifferentFrom(
    id: BookId,
    source: ListeningEvent.Source = ListeningEvent.Source.App,
  )

  fun pause()

  fun skipSilence(skip: Boolean)

  fun fastForward(source: ListeningEvent.Source = ListeningEvent.Source.App)

  fun rewind(source: ListeningEvent.Source = ListeningEvent.Source.App)

  fun previous()

  fun next()

  /** @param source recorded in the listening history, or null to not record it. */
  fun play(source: ListeningEvent.Source? = ListeningEvent.Source.App)

  fun playPause(source: ListeningEvent.Source = ListeningEvent.Source.App)

  /** Records an event at the current position in the listening history. */
  fun record(
    type: ListeningEvent.Type,
    source: ListeningEvent.Source,
  )

  fun pauseWithRewind(rewind: Duration)

  fun setSpeed(speed: Float)

  fun setGain(gain: Decibel)

  fun setVolume(volume: Float)

  suspend fun livePlaybackState(bookId: BookId? = null): LivePlaybackState?

  fun livePlaybackStateFlow(bookId: BookId? = null): Flow<LivePlaybackState?>
}
