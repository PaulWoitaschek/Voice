package de.ph1b.audiobook.data

data class BookMetaData(
  val id: Long = 0L,
  val type: Book.Type,
  val author: String?,
  val name: String,
  val root: String
) {

  init {
    require(name.isNotEmpty(), { "name must not be empty" })
    require(root.isNotEmpty(), { "root must not be empty" })
  }
}
