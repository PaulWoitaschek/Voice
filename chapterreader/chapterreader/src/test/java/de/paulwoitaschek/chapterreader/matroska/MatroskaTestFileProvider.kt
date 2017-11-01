package de.paulwoitaschek.chapterreader.matroska

import de.paulwoitaschek.chapterreader.Chapter
import java.io.File

object MatroskaTestFileProvider {

  private val file = javaClass.classLoader.getResource("matroska/simple.mka").file!!
  val testFile = File(file)
  internal val testFileMatroskaChapters = listOf(
    MatroskaChapter(
      0, listOf(
      MatroskaChapterName("Part 1", setOf("eng"))
    ), listOf()
    ),
    MatroskaChapter(
      2000000000L, listOf(
      MatroskaChapterName("Part 2", setOf("eng"))
    ), listOf(
      MatroskaChapter(
        2000000000L, listOf(
        MatroskaChapterName("Podczęść 1", setOf("pol")),
        MatroskaChapterName("Subpart 1", setOf("eng", "ger"))
      ), listOf()
      ),
      MatroskaChapter(
        3000000000L, listOf(
        MatroskaChapterName("Subpart 2", setOf("eng"))
      ), listOf()
      ),
      MatroskaChapter(4000000000L, listOf(), listOf())
    )
    )
  )
  val testFileChapters = listOf(
    Chapter(0, "Part 1"),
    Chapter(2000, "Part 2"),
    Chapter(2001, "+ Podczęść 1"),
    Chapter(3000, "+ Subpart 2"),
    Chapter(4000, "+ Chapter 3")
  )
}
