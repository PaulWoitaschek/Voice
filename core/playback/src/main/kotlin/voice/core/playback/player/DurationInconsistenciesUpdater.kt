package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import voice.core.data.repo.ChapterRepo
import voice.core.logging.api.Logger
import voice.core.playback.session.MediaId
import voice.core.playback.session.realChapterId
import voice.core.playback.session.toMediaIdOrNull

@Inject
class DurationInconsistenciesUpdater(
  private val chapterRepo: ChapterRepo,
  private val scope: CoroutineScope,
) : Player.Listener {

  private lateinit var player: Player

  fun attachTo(player: Player) {
    this.player = player
    player.addListener(this)
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    if (playbackState == Player.STATE_READY) {
      updateDurationIfNeeded()
    }
  }

  override fun onTimelineChanged(
    timeline: Timeline,
    reason: Int,
  ) {
    updateDurationIfNeeded()
  }

  private fun updateDurationIfNeeded() {
    val mediaId = player.currentMediaItem?.mediaId?.toMediaIdOrNull()
      ?: return
    val playerDuration = player.duration
    if (playerDuration == C.TIME_UNSET || playerDuration <= 0) return

    val chapterDuration = when (mediaId) {
      is MediaId.Chapter -> playerDuration
      is MediaId.ChapterMark -> {
        val endPositionMs = player.currentMediaItem?.clippingConfiguration?.endPositionMs
        if (endPositionMs != C.TIME_END_OF_SOURCE) return
        mediaId.startMs + playerDuration
      }
      else -> return
    }
    val chapterId = mediaId.realChapterId ?: return

    scope.launch {
      val chapter = chapterRepo.get(chapterId)
      if (chapter != null && chapter.duration != chapterDuration) {
        Logger.d(
          """For chapter=${chapter.id}, we had ${chapter.duration},
          |but the player reported $chapterDuration. Updating the chapter now
          """.trimMargin(),
        )
        chapterRepo.put(chapter.copy(duration = chapterDuration))
      }
    }
  }
}
