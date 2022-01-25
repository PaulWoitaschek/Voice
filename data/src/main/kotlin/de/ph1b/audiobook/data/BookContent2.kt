package de.ph1b.audiobook.data

import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.File
import java.time.Instant

@Entity(tableName = "content2")
data class BookContent2(
  @PrimaryKey
  val id: Book2.Id,
  val playbackSpeed: Float,
  val skipSilence: Boolean,
  val isActive: Boolean,
  val lastPlayedAt: Instant,
  val author: String?,
  val name: String,
  val addedAt: Instant,
  val chapters: List<Chapter2.Id>,
  val currentChapter: Chapter2.Id,
  val positionInChapter: Long,
  val cover: File?,
) {

  val uri: Book2.Id get() = id

  @Ignore
  val currentChapterIndex = chapters.indexOf(currentChapter).also { require(it != -1) }

  init {
    require(currentChapter in chapters)
  }
}
