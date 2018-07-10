package de.ph1b.audiobook.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.io.File
import java.util.UUID

@Entity(tableName = "bookSettings")
data class BookSettings(
  @ColumnInfo(name = "id")
  @PrimaryKey
  val id: UUID,
  @ColumnInfo(name = "currentFile")
  val currentFile: File,
  @ColumnInfo(name = "positionInChapter")
  val positionInChapter: Int,
  @ColumnInfo(name = "playbackSpeed")
  val playbackSpeed: Float = 1F,
  @ColumnInfo(name = "loudnessGain")
  val loudnessGain: Int = 0,
  @ColumnInfo(name = "skipSilence")
  val skipSilence: Boolean = false,
  @ColumnInfo(name = "active")
  val active: Boolean,
  @ColumnInfo(name = "lastPlayedAtMillis")
  val lastPlayedAtMillis: Long
) {

  init {
    require(playbackSpeed >= Book.SPEED_MIN) { "speed $playbackSpeed must be >= ${Book.SPEED_MIN}" }
    require(playbackSpeed <= Book.SPEED_MAX) { "speed $playbackSpeed must be <= ${Book.SPEED_MAX}" }
    require(positionInChapter >= 0) { "positionInChapter must not be negative" }
    require(loudnessGain >= 0) { "loudnessGain must not be negative" }
  }
}
