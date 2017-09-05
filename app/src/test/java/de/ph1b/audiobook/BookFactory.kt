package de.ph1b.audiobook

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import java.io.File
import java.util.*

object BookFactory {

  private val rnd = Random()

  fun create(
      file1: File = File("/root/", "First.mp3"),
      file2: File = File("/root/", "/second.mp3"),
      id: Long = -1
  ): Book {
    val type = Book.Type.SINGLE_FOLDER
    val author = "TestAuthor"
    val time = 0
    val name = "TestBook"
    val marks1 = SparseArray<String>().apply {
      put(0, "first")
      put(5000, "second")
    }
    val chapter1 = Chapter(file1, file1.name, 1 + rnd.nextInt(100000), 13534, marks1)
    val chapter2 = Chapter(file2, file2.name, 1 + rnd.nextInt(200000), 134134, emptySparseArray())
    val chapters = listOf(chapter1, chapter2)
    val playbackSpeed = 1f
    var root: String? = file1.parent
    if (root == null) {
      root = "fakeRoot"
    }
    return Book(
        id = id,
        type = type,
        author = author,
        currentFile = file1,
        time = time,
        name = name,
        chapters = chapters,
        playbackSpeed = playbackSpeed,
        root = root,
        loudnessGain = 500
    )
  }
}
