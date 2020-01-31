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
  object PlayPause : PlayerCommand()

  @Parcelize
  data class SkipSilence(val skipSilence: Boolean) : PlayerCommand()

  @Parcelize
  data class SetLoudnessGain(val mB: Int) : PlayerCommand()

  @Parcelize
  data class SetPlaybackSpeed(val speed: Float) : PlayerCommand()

  @Parcelize
  data class SetPosition(val time: Long, val file: File) : PlayerCommand()

  @Parcelize
  data class Seek(val time: Long) : PlayerCommand()

  @Parcelize
  data class PlayChapterAtIndex(val index: Long) : PlayerCommand()

  fun toServiceIntent(context: Context): Intent {
    return toIntent<PlaybackService>(context)
  }

  private inline fun <reified T> toIntent(context: Context): Intent {
    return Intent(context, T::class.java)
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
