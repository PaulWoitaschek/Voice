package de.ph1b.audiobook.data

import android.content.Context
import android.os.Environment
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import de.ph1b.audiobook.data.repo.internals.IO
import kotlinx.coroutines.experimental.withContext
import java.io.File


data class Book(
  val id: Long,
  val type: Type,
  val author: String?,
  val currentFile: File,
  val positionInChapter: Int,
  val name: String,
  val chapters: List<Chapter>,
  val playbackSpeed: Float = 1F,
  val root: String,
  val loudnessGain: Int = 0
) : Comparable<Book> {

  init {
    require(playbackSpeed >= SPEED_MIN, { "speed $playbackSpeed must be >= $SPEED_MIN" })
    require(playbackSpeed <= SPEED_MAX) { "speed $playbackSpeed must be <= $SPEED_MAX" }
    require(name.isNotEmpty(), { "name must not be empty" })
    require(root.isNotEmpty(), { "root must not be empty" })
    require(positionInChapter >= 0) { "positionInChapter must not be negative" }
    require(loudnessGain >= 0) { "loudnessGain must not be negative" }
  }

  val coverTransitionName = "bookCoverTransition_$id"
  val currentChapter = chapters.first { it.file == currentFile }
  val currentChapterIndex = chapters.indexOf(currentChapter)
  val nextChapter = chapters.getOrNull(currentChapterIndex + 1)
  val previousChapter = chapters.getOrNull(currentChapterIndex - 1)
  val position: Int = chapters.take(currentChapterIndex).sumBy { it.duration } + positionInChapter
  val duration = chapters.sumBy { it.duration }

  val nextChapterMarkPosition: Int? by lazy {
    currentChapter.marks.forEachIndexed { _, start, _ ->
      if (start > positionInChapter) return@lazy start
    }
    null
  }

  suspend fun coverFile(context: Context): File = withContext(IO) {
    val name = type.name + if (type == Type.COLLECTION_FILE || type == Type.COLLECTION_FOLDER) {
      // if its part of a collection, take the first file
      chapters.first().file.absolutePath.replace("/", "")
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
    const val ID_UNKNOWN = -1L
    const val SPEED_MAX = 2.5F
    const val SPEED_MIN = 0.5F
  }
}
