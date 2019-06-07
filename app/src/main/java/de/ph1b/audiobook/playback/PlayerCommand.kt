package de.ph1b.audiobook.playback

import android.content.Context
import android.content.Intent
import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import java.io.File

sealed class PlayerCommand : Parcelable {

  @Parcelize
  object Play : PlayerCommand()

  @Parcelize
  object Next : PlayerCommand()

  @Parcelize
  object PlayPause : PlayerCommand()

  @Parcelize
  object Rewind : PlayerCommand()

  @Parcelize
  object RewindAutoPlay : PlayerCommand()

  @Parcelize
  object FastForward : PlayerCommand()

  @Parcelize
  object Previous : PlayerCommand()

  @Parcelize
  data class SkipSilence(val skipSilence: Boolean) : PlayerCommand()

  @Parcelize
  data class SetLoudnessGain(val mB: Int) : PlayerCommand()

  @Parcelize
  data class SetPlaybackSpeed(val speed: Float) : PlayerCommand()

  @Parcelize
  data class SetPosition(val time: Int, val file: File) : PlayerCommand()

  @Parcelize
  data class PlayChapterAtIndex(val index: Long) : PlayerCommand()

  @Parcelize
  object FastForwardAutoPlay : PlayerCommand()

  @Parcelize
  object Stop : PlayerCommand()

  fun toIntent(context: Context): Intent {
    return Intent(context, PlaybackService::class.java)
      .setAction(INTENT_ACTION)
      .putExtra(INTENT_EXTRA, this)
  }

  companion object {

    const val INTENT_ACTION = "de.ph1b.audiobook.playeraction"
    const val INTENT_EXTRA = "de.ph1b.audiobook.playeraction#extra"

    fun fromIntent(intent: Intent): PlayerCommand? {
      return if (intent.action == INTENT_ACTION) {
        intent.getParcelableExtra(INTENT_EXTRA)!!
      } else {
        null
      }
    }
  }
}
