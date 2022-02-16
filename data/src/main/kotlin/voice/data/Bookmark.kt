package voice.data

import android.os.Bundle
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import java.util.UUID

@Entity(tableName = "bookmark2")
data class Bookmark(
  val bookId: Book.Id,
  val chapterId: Chapter.Id,
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

fun Bundle.putBookmarkId(key: String, value: Bookmark.Id) {
  putString(key, value.value.toString())
}

fun Bundle.getBookmarkId(key: String): Bookmark.Id? {
  val value = getString(key) ?: return null
  return Bookmark.Id(UUID.fromString(value))
}
