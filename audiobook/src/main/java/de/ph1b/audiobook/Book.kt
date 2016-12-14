package de.ph1b.audiobook


import android.os.Environment
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.NaturalOrderComparator
import i
import java.io.File


/**
 * Represents a playable book.
 *
 * @param id the book id
 * @param author the author of the book.
 *
 * @author Paul Woitaschek
 */
data class Book(
  val id: Long,
  val type: Type,
  val author: String?,
  val currentFile: File,
  val time: Int,
  val name: String,
  val chapters: List<Chapter>,
  val playbackSpeed: Float,
  val root: String) : Comparable<Book> {

  override fun compareTo(other: Book) = NaturalOrderComparator.stringComparator.compare(name, other.name)

  companion object {
    const val ID_UNKNOWN = -1L
    const val SPEED_MAX = 2.5F
    const val SPEED_MIN = 0.5F
  }

  private val COVER_TRANSITION_PREFIX = "bookCoverTransition_"

  init {
    check(playbackSpeed >= SPEED_MIN, { "speed $playbackSpeed must be >= $SPEED_MIN" })
    check(playbackSpeed <= SPEED_MAX) { "speed $playbackSpeed must be <= $SPEED_MAX" }
    check(chapters.isNotEmpty(), { "chapters must not be empty" })
    check(chapters.find { it.file == currentFile } != null, { "$chapters must contain current $currentFile" })
    check(name.isNotEmpty(), { "name must not be empty" })
    check(root.isNotEmpty(), { "root must not be empty" })
  }

  /** The transition name for the cover transition. */
  val coverTransitionName = COVER_TRANSITION_PREFIX + id

  /** The global duration. It sums up the duration of all chapters. */
  val globalDuration: Int by lazy {
    var globalDuration = 0
    for ((file, name, duration) in chapters) {
      globalDuration += duration
    }
    globalDuration
  }

  /**
   * @return the global position. It sums up the duration of all elapsed chapters plus the position
   * * in the current chapter.
   */
  fun globalPosition(): Int {
    var globalPosition = 0
    for (c in chapters) {
      if (c == currentChapter()) {
        globalPosition += time
        return globalPosition
      } else {
        globalPosition += c.duration
      }
    }
    throw IllegalStateException("Current chapter was not found while looking up the global position")
  }

  fun currentChapter() = chapters.first { it.file == currentFile }

  fun nextChapter(): Chapter? {
    val currentIndex = chapters.indexOf(currentChapter())
    if (currentIndex < chapters.size - 1) {
      return chapters[currentIndex + 1]
    }
    return null
  }

  fun previousChapter(): Chapter? {
    val currentIndex = chapters.indexOf(currentChapter())
    if (currentIndex > 0) {
      return chapters[currentIndex - 1]
    }
    return null
  }

  fun coverFile(): File {
    val separator = File.separator

    val name = type.name + if (type == Type.COLLECTION_FILE || type == Type.COLLECTION_FOLDER) {
      // if its part of a collection, take the first file
      chapters.first().file.absolutePath.replace(separator, "")
    } else {
      // if its a single, just take the root
      root.replace(separator, "")
    } + ".jpg"

    val coverFile = File("${Environment.getExternalStorageDirectory().absolutePath}${separator}Android${separator}data$separator${App.component.context.packageName}",
      name)
    i { "CoverFile is $coverFile" }
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
