package de.ph1b.audiobook

import java.io.File
import java.util.*

/**
 * Mock provider for mocking objects and injecting them.
 *
 * @author Paul Woitaschek
 */
object BookMocker {

  private val rnd = Random()

  fun mock(id: Long): Book {
    val root = "/root/"
    return mock(File(root, "First.mp3"), File(root, "/second.mp3"), id)
  }

  fun mock(file1: File, file2: File, id: Long): Book {
    val type = Book.Type.SINGLE_FOLDER
    val author = "TestAuthor"
    val time = 0
    val name = "TestBook"
    val chapter1 = Chapter(file1, file1.name, 1 + rnd.nextInt(100000), 13534)
    val chapter2 = Chapter(file2, file2.name, 1 + rnd.nextInt(200000), 134134)
    val chapters = listOf(chapter1, chapter2)
    val playbackSpeed = 1f
    var root: String? = file1.parent
    if (root == null) {
      root = "fakeRoot"
    }
    return Book(id, type, author, file1, time, name, chapters, playbackSpeed, root)
  }
}
