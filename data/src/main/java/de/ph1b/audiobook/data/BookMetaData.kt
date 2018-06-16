package de.ph1b.audiobook.data

import android.arch.persistence.room.ColumnInfo
import android.arch.persistence.room.Entity
import android.arch.persistence.room.PrimaryKey
import java.util.UUID

@Entity(tableName = "bookMetaData")
data class BookMetaData(
  @ColumnInfo(name = "id")
  @PrimaryKey
  val id: UUID,
  @ColumnInfo(name = "type")
  val type: Book.Type,
  @ColumnInfo(name = "author")
  val author: String?,
  @ColumnInfo(name = "name")
  val name: String,
  @ColumnInfo(name = "root")
  val root: String
) {

  init {
    require(name.isNotEmpty(), { "name must not be empty" })
    require(root.isNotEmpty(), { "root must not be empty" })
  }
}
