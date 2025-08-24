package voice.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import voice.common.BookId
import java.time.Instant
import java.util.UUID

@Entity(tableName = "bookmark2")
public data class Bookmark(
  val bookId: BookId,
  val chapterId: ChapterId,
  val title: String?,
  val time: Long,
  val addedAt: Instant,
  val setBySleepTimer: Boolean,
  @PrimaryKey
  val id: Id,
) {

  public data class Id(val value: UUID) {
    public companion object {
      public fun random(): Id = Id(UUID.randomUUID())
    }
  }
}
