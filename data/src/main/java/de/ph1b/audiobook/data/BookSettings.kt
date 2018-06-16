package de.ph1b.audiobook.data

import java.io.File
import java.util.UUID

data class BookSettings(
  val id: UUID,
  val currentFile: File,
  val positionInChapter: Int,
  val playbackSpeed: Float = 1F,
  val loudnessGain: Int = 0,
  val skipSilence: Boolean = false
) {

  init {
    require(playbackSpeed >= Book.SPEED_MIN) { "speed $playbackSpeed must be >= ${Book.SPEED_MIN}" }
    require(playbackSpeed <= Book.SPEED_MAX) { "speed $playbackSpeed must be <= ${Book.SPEED_MAX}" }
    require(positionInChapter >= 0) { "positionInChapter must not be negative" }
    require(loudnessGain >= 0) { "loudnessGain must not be negative" }
  }
}
