package voice.data.legacy

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bookMetaData")
data class LegacyBookMetaData(
  @ColumnInfo(name = "id")
  @PrimaryKey
  val id: UUID,
  @ColumnInfo(name = "type")
  val type: LegacyBookType,
  @ColumnInfo(name = "author")
  val author: String?,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "root")
  val root: String,
  @ColumnInfo(name = "addedAtMillis")
  val addedAtMillis: Long,
) {

  init {
    require(name.isNotEmpty()) { "name must not be empty" }
    require(root.isNotEmpty()) { "root must not be empty" }
  }
}
