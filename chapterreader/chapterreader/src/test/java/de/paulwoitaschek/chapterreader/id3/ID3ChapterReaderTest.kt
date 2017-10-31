package de.paulwoitaschek.chapterreader.id3

import com.google.common.truth.Truth.assertThat
import org.junit.Test
import java.io.File

class ID3ChapterReaderTest {

  private val id3ChapterReader = ID3ChapterReader()

  @Test
  fun read() {
    val file = File(javaClass.classLoader.getResource("id3/simple.mp3").file)

    val chapters = id3ChapterReader.read(file)

    assertThat(chapters).isEqualTo(
      mapOf(
        0 to "Intro",
        15000 to "Creating a new production",
        22000 to "Sound analysis",
        34000 to "Adaptive leveler",
        45000 to "Global loudness normalization",
        60000 to "Audio restoration algorithms",
        76000 to "Output file formats",
        94000 to "External services",
        111500 to "Get a free account!"
      )
    )
  }
}
