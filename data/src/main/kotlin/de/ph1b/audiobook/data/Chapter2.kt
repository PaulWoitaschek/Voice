package de.ph1b.audiobook.data

import android.net.Uri
import androidx.core.net.toUri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
import java.time.Instant

@Entity(tableName = "chapters2")
data class Chapter2(
  @PrimaryKey
  val id: Id,
  val name: String,
  val duration: Long,
  val fileLastModified: Instant,
  val markData: List<MarkData>,
) {

  init {
    require(name.isNotEmpty())
  }

  @Ignore
  val chapterMarks: List<ChapterMark> = if (markData.isEmpty()) {
    listOf(ChapterMark(name, 0L, duration))
  } else {
    val sorted = markData.sorted()
    sorted.mapIndexed { index, (startMs, name) ->
      val isFirst = index == 0
      val isLast = index == sorted.size - 1
      val start = if (isFirst) 0L else startMs
      val end = if (isLast) duration else sorted[index + 1].startMs - 1
      ChapterMark(name = name, startMs = start, endMs = end)
    }
  }

  @Serializable
  data class Id(val value: String) {

    constructor(uri: Uri) : this(uri.toString())
  }
}

fun Chapter2.Id.toUri(): Uri = value.toUri()
