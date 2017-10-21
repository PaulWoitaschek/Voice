package de.ph1b.audiobook

import android.support.v4.util.SparseArrayCompat
import de.ph1b.audiobook.common.sparseArray.emptySparseArray
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.Chapter
import java.io.File
import java.util.Random

object BookFactory {

  private val rnd = Random()

  fun chapter(file: String = "First.mp3",
              parent: String = "/root/",
              duration: Int = 1 + rnd.nextInt(100000),
              lastModified: Long = 12345L,
              marks: SparseArrayCompat<String> = emptySparseArray()): Chapter =
      Chapter(File(parent, file), file, duration, lastModified, marks)

  fun newBook(
      id: Long = -1,
      type: Book.Type = Book.Type.SINGLE_FOLDER,
      author: String = "TestAuthor",
      currentFileIndex: Int = 0,
      time: Int = 0,
      name: String = "TestBook",
      playbackSpeed: Float = 1F,
      loudnessGain: Int = 500,
      chapters: List<Chapter> = listOf(chapter())): Book {
    val currentFile = chapters[currentFileIndex].file
    val root = if (currentFile.parent != null) currentFile.parent else "fakeRoot"

    return Book(id, type, author, currentFile, time, name, chapters, playbackSpeed, root, loudnessGain)
  }

  fun create(
      id: Long = -1,
      time: Int = 0
  ): Book {
    val marks1 = SparseArrayCompat<String>().apply {
      put(0, "first")
      put(5000, "second")
    }
    return newBook(
        id = id,
        time = time,
        currentFileIndex = 0,
        chapters = listOf(
            chapter(file = "First.mp3", lastModified = 13534, marks = marks1),
            chapter(file = "/second.mp3", lastModified = 134134)
        )
    )
  }
}
