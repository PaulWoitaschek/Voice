package voice.core.data

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.File

@Entity(tableName = "customMetadata")
public data class CustomMetadata(
  @PrimaryKey
  val bookId: BookId,
  val name: String? = null,
  val author: String? = null,
  val cover: File? = null,
  val series: String? = null,
  val part: String? = null,
) {

  public fun followingFiles(metadata: BookMetadata): CustomMetadata {
    // the names from the files aren't kept, so the book follows them when they change
    return copy(
      name = name?.takeIf { it != metadata.name },
      author = author?.takeIf { it != metadata.author },
      series = series?.takeIf { it != metadata.series },
      part = part?.takeIf { it != metadata.part },
    )
  }
}
