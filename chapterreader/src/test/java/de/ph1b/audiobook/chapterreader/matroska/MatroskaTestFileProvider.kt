package de.ph1b.audiobook.chapterreader.matroska

import java.io.File


object MatroskaTestFileProvider {

  val file = javaClass.classLoader.getResource("matroska/simple.mka").file
  val testFile = File(file)
  val testFileMatroskaChapters = listOf(
      MatroskaChapter(0, listOf(
          MatroskaChapterName("Part 1", setOf("eng"))
      ), listOf()),
      MatroskaChapter(2000000000L, listOf(
          MatroskaChapterName("Part 2", setOf("eng"))
      ), listOf(
          MatroskaChapter(2000000000L, listOf(
              MatroskaChapterName("Podczęść 1", setOf("pol")),
              MatroskaChapterName("Subpart 1", setOf("eng", "ger"))
          ), listOf()),
          MatroskaChapter(3000000000L, listOf(
              MatroskaChapterName("Subpart 2", setOf("eng"))
          ), listOf()),
          MatroskaChapter(4000000000L, listOf(), listOf())
      ))
  )
  val testFileChapters = mapOf(
      0 to "Part 1",
      2000 to "Part 2",
      2001 to "+ Podczęść 1",
      3000 to "+ Subpart 2",
      4000 to "+ Chapter 3"
  )
}
