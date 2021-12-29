package de.ph1b.audiobook.data

import android.content.Context
import java.io.File
import java.util.UUID

data class Book(
  val id: UUID,
  val content: BookContent,
  val metaData: BookMetaData
) {

  init {
    require(content.id == id) { "wrong book content" }
    require(metaData.id == id) { "Wrong metaData for $this" }
  }

  val type = metaData.type
  val author = metaData.author
  val name = metaData.name
  val root = metaData.root

  fun updateMetaData(update: BookMetaData.() -> BookMetaData): Book = copy(
    metaData = update(metaData)
  )

  val coverTransitionName = "bookCoverTransition_$id"

  inline fun updateContent(update: BookContent.() -> BookContent): Book = copy(
    content = update(content)
  )

  inline fun update(
    updateContent: BookContent.() -> BookContent = { this },
    updateMetaData: BookMetaData.() -> BookMetaData = { this },
    updateSettings: BookSettings.() -> BookSettings = { this }
  ): Book {
    val newSettings = updateSettings(content.settings)
    val contentWithNewSettings = if (newSettings === content.settings) {
      content
    } else {
      content.copy(
        settings = newSettings
      )
    }
    val newContent = updateContent(contentWithNewSettings)
    val newMetaData = updateMetaData(metaData)
    return copy(content = newContent, metaData = newMetaData)
  }

  fun coverFile(context: Context): File {
    return bookCover(context, id)
  }

  enum class Type {
    COLLECTION_FOLDER,
    COLLECTION_FILE,
    SINGLE_FOLDER,
    SINGLE_FILE
  }

  companion object {
    const val SPEED_MAX = 2.5F
    const val SPEED_MIN = 0.5F
  }
}

fun bookCover(context: Context, bookId: UUID): File {
  return File(context.filesDir, bookId.toString())
}
