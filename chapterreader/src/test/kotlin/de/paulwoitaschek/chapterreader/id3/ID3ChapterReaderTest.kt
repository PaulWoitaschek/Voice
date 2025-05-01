package de.paulwoitaschek.chapterreader.id3

import de.paulwoitaschek.chapterreader.Chapter
import io.kotest.matchers.collections.shouldContainExactly
import org.junit.Test
import java.io.File

class ID3ChapterReaderTest {

  private val id3ChapterReader = ID3ChapterReader()

  @Test
  fun read() {
    val file = File(javaClass.classLoader!!.getResource("id3/simple.mp3").file)

    val chapters = id3ChapterReader.read(file)

    chapters.shouldContainExactly(
      Chapter(0L, "Intro"),
      Chapter(15000L, "Creating a new production"),
      Chapter(22000, "Sound analysis"),
      Chapter(34000, "Adaptive leveler"),
      Chapter(45000, "Global loudness normalization"),
      Chapter(60000, "Audio restoration algorithms"),
      Chapter(76000, "Output file formats"),
      Chapter(94000, "External services"),
      Chapter(111500, "Get a free account!"),
    )
  }
}
