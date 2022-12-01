package voice.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import voice.common.BookId
import java.io.File
import java.time.Instant

@Entity(tableName = "content2")
data class BookContent(
  @PrimaryKey
  val id: BookId,
  val playbackSpeed: Float,
  val skipSilence: Boolean,
  val isActive: Boolean,
  val lastPlayedAt: Instant,
  val author: String?,
  val name: String,
  val addedAt: Instant,
  val chapters: List<ChapterId>,
  val currentChapter: ChapterId,
  val positionInChapter: Long,
  val cover: File?,
  @ColumnInfo(defaultValue = "0")
  val gain: Float,
) {

  @Ignore
  val currentChapterIndex = chapters.indexOf(currentChapter).also { require(it != -1) }

  init {
    require(currentChapter in chapters)
    require(positionInChapter >= 0)
  }
}
