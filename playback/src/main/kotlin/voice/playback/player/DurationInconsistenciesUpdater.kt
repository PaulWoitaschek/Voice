package voice.playback.player

import androidx.media3.common.Player
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.data.repo.ChapterRepo
import voice.logging.core.Logger
import voice.playback.session.MediaId
import voice.playback.session.toMediaIdOrNull
import javax.inject.Inject

class DurationInconsistenciesUpdater
@Inject constructor(private val chapterRepo: ChapterRepo) : Player.Listener {

  private lateinit var player: Player

  private val scope = MainScope()

  fun attachTo(player: Player) {
    this.player = player
    player.addListener(this)
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    if (playbackState != Player.STATE_READY) return
    val mediaId = player.currentMediaItem?.mediaId?.toMediaIdOrNull()
      ?: return
    if (mediaId is MediaId.Chapter) {
      val playerDuration = player.duration
      scope.launch {
        val chapter = chapterRepo.get(mediaId.chapterId)
        if (chapter != null && chapter.duration != playerDuration) {
          Logger.d(
            """For chapter=${chapter.id}, we had ${chapter.duration},
            |but the player reported $playerDuration. Updating the chapter now
            """.trimMargin(),
          )
          chapterRepo.put(chapter.copy(duration = playerDuration))
        }
      }
    }
  }
}
