package de.ph1b.audiobook

import android.util.SparseArray
import de.ph1b.audiobook.misc.emptySparseArray
import java.io.File
import java.util.*

/**
 * Mock provider for mocking objects and injecting them.
 *
 * @author Paul Woitaschek
 */
object BookMocker {

  private val rnd = Random()

  fun mock(): Book {
    val root = "/root/"
    return mock(File(root, "First.mp3"), File(root, "/second.mp3"))
  }

  fun mock(file1: File, file2: File): Book {
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
    return Book(-1, type, author, file1, time, name, chapters, playbackSpeed, root)
  }
}
