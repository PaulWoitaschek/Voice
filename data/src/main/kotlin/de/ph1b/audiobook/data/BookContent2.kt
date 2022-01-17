package de.ph1b.audiobook.data

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.File
import java.time.Instant

@Entity(tableName = "content2")
data class BookContent2(
  @PrimaryKey
  val uri: Uri,
  val playbackSpeed: Float,
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
  val positionInChapter: Long,
  val cover: File?,
) {

  @Ignore
  val currentChapterIndex = chapters.indexOf(currentChapter).also { require(it != -1) }

  init {
    require(currentChapter in chapters)
  }

  enum class Type {
    File, Folder
  }
}
