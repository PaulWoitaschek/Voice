package de.ph1b.audiobook.data

import android.os.Bundle
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "bookmark2")
data class Bookmark2(
  val bookId: Book2.Id,
  val chapterId: Chapter2.Id,
  val title: String?,
  val time: Long,
  val addedAt: Instant,
  val setBySleepTimer: Boolean,
  @PrimaryKey
  val id: Id
) {

  data class Id(val value: UUID) {
    companion object {
      fun random(): Id = Id(UUID.randomUUID())
    }
  }
}

fun Bundle.putBookmarkId(key: String, value: Bookmark2.Id) {
  putString(key, value.value.toString())
}

fun Bundle.getBookmarkId(key: String): Bookmark2.Id? {
  val value = getString(key) ?: return null
  return Bookmark2.Id(UUID.fromString(value))
}
