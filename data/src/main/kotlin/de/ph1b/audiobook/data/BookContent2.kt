package de.ph1b.audiobook.data

import android.net.Uri
import java.time.Instant

data class BookContent2(
  val uri: Uri,
  val playbackSpeed: Float,
  val loudnessGain: Int,
  val skipSilence: Boolean,
  val isActive: Boolean,
  val lastPlayedAt: Instant,
  val type: Type,
  val author: String?,
  val name: String,
  val addedAt: Instant,
  val position: Long,
  val duration: Long,
  val chapters: List<Uri>,
  val currentChapter: Uri,
  val positionInChapter: Long
) {

  init {
    require(currentChapter in chapters)
  }

  enum class Type {
    File, Folder
  }
}
