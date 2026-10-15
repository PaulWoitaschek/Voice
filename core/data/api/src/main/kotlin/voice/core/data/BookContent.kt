package voice.core.data

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import java.io.File
import java.time.Instant

@Entity(tableName = "content2")
public data class BookContent(
  @PrimaryKey
  val id: BookId,
  val playbackSpeed: Float,
  val skipSilence: Boolean,
  val isActive: Boolean,
  val lastPlayedAt: Instant,
  val addedAt: Instant,
  val chapters: List<ChapterId>,
  val currentChapter: ChapterId,
  val positionInChapter: Long,
  @ColumnInfo(defaultValue = "0")
  val gain: Float,
  @Embedded
  val metadata: BookMetadata,
  val customName: String?,
  val customCover: File?,
) {

  @Ignore
  val currentChapterIndex: Int = chapters.indexOf(currentChapter)

  val name: String get() = customName ?: metadata.name

  val cover: File? get() = customCover ?: metadata.cover

  val coverUrl: String? get() = cover?.toURI()?.toString()

  init {
    require(currentChapter in chapters && positionInChapter >= 0) {
      "Invalid data: chapter $currentChapterIndex of ${chapters.size}, position $positionInChapter"
    }
  }

  public fun withCustomName(name: String?): BookContent {
    // the name from the files isn't kept, so the book follows them when they change
    return copy(customName = name?.takeIf { it != metadata.name })
  }

  public fun withMetadata(metadata: BookMetadata): BookContent {
    return copy(metadata = metadata).withCustomName(customName)
  }
}

public data class BookMetadata(
  val name: String,
  val author: String?,
  val album: String?,
  val title: String?,
  val genre: String?,
  val narrator: String?,
  val series: String?,
  val part: String?,
  val cover: File?,
  // null for books on a server, and for books from before it was stored, which the scan reads again
  @ColumnInfo(name = "metadataFileLastModified")
  val fileLastModified: Instant?,
)
