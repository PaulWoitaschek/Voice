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
  /** The name the listener gave the book, over the one from its files. */
  val customName: String?,
  /** The cover the listener picked, over the one from its files. */
  val customCover: File?,
) {

  @Ignore
  val currentChapterIndex: Int = chapters.indexOf(currentChapter)

  val name: String get() = customName ?: metadata.name

  val cover: File? get() = customCover ?: metadata.cover

  val coverUrl: String? get() = cover?.toURI()?.toString()

  init {
    require(currentChapter in chapters && positionInChapter >= 0) {
      "invalid data in $this"
    }
  }

  /**
   * The book with the name the listener gave it. Giving it the name from its files again lets it follow its files.
   */
  public fun withCustomName(name: String?): BookContent {
    return copy(customName = name?.takeIf { it != metadata.name })
  }

  /**
   * The book with what its files say now. A name the listener gave it stays, unless the files say the same now.
   */
  public fun withMetadata(metadata: BookMetadata): BookContent {
    return copy(metadata = metadata).withCustomName(customName)
  }
}

/**
 * What the files of a book, or the server it's on, say about it. Kept apart from what the listener changed, so a scan
 * can read it again when the files change.
 */
public data class BookMetadata(
  /** What Voice makes of the tags, or the name of the folder or file without them. */
  val name: String,
  val author: String?,
  /** The tags of the first file. `null` for books on a server. */
  val album: String?,
  val title: String?,
  val genre: String?,
  val narrator: String?,
  val series: String?,
  val part: String?,
  val cover: File?,
  /**
   * When the first file was last modified as this was read from it. `null` for books on a server and for books
   * stored before this was kept, which a scan reads again.
   */
  @ColumnInfo(name = "metadataFileLastModified")
  val fileLastModified: Instant?,
)
