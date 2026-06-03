package voice.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.time.Instant
import kotlin.uuid.Uuid

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

  public data class Id(val value: Uuid) {
    public companion object {
      public fun random(): Id = Id(Uuid.random())
    }
  }
}
