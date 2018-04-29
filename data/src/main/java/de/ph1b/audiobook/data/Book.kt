package de.ph1b.audiobook.data

import android.content.Context
import android.os.Environment
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.data.repo.internals.IO
import kotlinx.coroutines.experimental.withContext
import java.io.File


data class Book(
  val id: Long = 0L,
  val content: BookContent,
  val metaData: BookMetaData
) : Comparable<Book> {

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

  suspend fun coverFile(context: Context): File = withContext(IO) {
    val name = type.name + if (type == Type.COLLECTION_FILE || type == Type.COLLECTION_FOLDER) {
      // if its part of a collection, take the first file
      content.chapters.first().file.absolutePath.replace("/", "")
    } else {
      // if its a single, just take the root
      root.replace("/", "")
    } + ".jpg"

    val externalStoragePath = Environment.getExternalStorageDirectory().absolutePath
    val coverFile = File(
      "$externalStoragePath/Android/data/${context.packageName}",
      name
    )
    if (!coverFile.parentFile.exists()) {
      //noinspection ResultOfMethodCallIgnored
      coverFile.parentFile.mkdirs()
    }
    coverFile
  }

  override fun compareTo(other: Book) =
    NaturalOrderComparator.stringComparator.compare(name, other.name)

  enum class Type {
    COLLECTION_FOLDER,
    COLLECTION_FILE,
    SINGLE_FOLDER,
    SINGLE_FILE
  }

  companion object {
    const val ID_UNKNOWN = 0L
    const val SPEED_MAX = 2.5F
    const val SPEED_MIN = 0.5F
  }
}
