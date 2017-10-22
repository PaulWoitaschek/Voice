package de.ph1b.audiobook.data

import android.content.Context
import android.os.Environment
import de.ph1b.audiobook.common.comparator.NaturalOrderComparator
import de.ph1b.audiobook.common.sparseArray.forEachIndexed
import java.io.File


data class Book(
    val id: Long,
    val type: Type,
    val author: String?,
    val currentFile: File,
    val time: Int,
    val name: String,
    val chapters: List<Chapter>,
    val playbackSpeed: Float = 1F,
    val root: String,
    val loudnessGain: Int = 0
) : Comparable<Book> {

  override fun compareTo(other: Book) = NaturalOrderComparator.stringComparator.compare(name, other.name)

  companion object {
    const val ID_UNKNOWN = -1L
    const val SPEED_MAX = 2.5F
    const val SPEED_MIN = 0.5F
  }

  private val COVER_TRANSITION_PREFIX = "bookCoverTransition_"

  init {
    require(playbackSpeed >= SPEED_MIN, { "speed $playbackSpeed must be >= ${SPEED_MIN}" })
    require(playbackSpeed <= SPEED_MAX) { "speed $playbackSpeed must be <= ${SPEED_MAX}" }
    require(chapters.isNotEmpty(), { "chapters must not be empty" })
    require(chapters.find { it.file == currentFile } != null, { "$chapters must contain current $currentFile" })
    require(name.isNotEmpty(), { "name must not be empty" })
    require(root.isNotEmpty(), { "root must not be empty" })
    require(time >= 0) { "time must not be negative" }
    require(loudnessGain >= 0) { "loudnessGain must not be negative" }
  }

  /** The transition name for the cover transition. */
  val coverTransitionName = COVER_TRANSITION_PREFIX + id

  /** The global duration. It sums up the duration of all chapters. */
  val globalDuration: Int by lazy {
    chapters.sumBy { it.duration }
  }

  /**
   * @return the global position. It sums up the duration of all elapsed chapters plus the position
   * in the current chapter.
   */
  val globalPosition: Int = chapters.takeWhile { it != currentChapter() }.sumBy { it.duration } + time

  fun currentChapter() = chapters.first { it.file == currentFile }

  fun nextChapterMarkPosition(): Int? {
    currentChapter().marks.forEachIndexed { _, start, _ ->
      if (start > time) return start
    }
    return null
  }

  fun nextChapter() = chapters.getOrNull(currentChapterIndex() + 1)

  fun currentChapterIndex() = chapters.indexOf(currentChapter())

  fun previousChapter() = chapters.getOrNull(currentChapterIndex() - 1)

  fun coverFile(context: Context): File {
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
    return coverFile
  }

  enum class Type {
    COLLECTION_FOLDER,
    COLLECTION_FILE,
    SINGLE_FOLDER,
    SINGLE_FILE
  }
}
