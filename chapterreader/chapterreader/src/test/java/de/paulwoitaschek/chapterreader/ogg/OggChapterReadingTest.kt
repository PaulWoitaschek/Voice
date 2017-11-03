package de.paulwoitaschek.chapterreader.ogg

import com.google.common.truth.Truth.assertThat
import de.paulwoitaschek.chapterreader.Chapter
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.File

class OggChapterReadingTest {

  private lateinit var oggChapterReader: OggChapterReader

  @BeforeEach
  fun setUp() {
    oggChapterReader = OggChapterReader()
  }

  @Test
  fun readChaptersFromOggOpusTest() {
    val file = File(javaClass.classLoader.getResource("ogg/simple.opus").file)
    val chapters = oggChapterReader.read(file)

    assertThat(chapters).containsExactly(
      Chapter(0, "Chapter 1"),
      Chapter(1000, "Chapter 2"),
      Chapter(2000, "Chapter 3"),
      Chapter(3000, "Chapter 4")
    )
  }

  @Test
  fun readChaptersFromOggVorbisTest() {
    val file = File(javaClass.classLoader.getResource("ogg/simple_vorbis.ogg").file)
    val chapters = oggChapterReader.read(file)

    assertThat(chapters).containsExactly(
      Chapter(0, "Part 1"),
      Chapter(20, "Part 2"),
      Chapter(2000, "Part 3")
    )
  }
}
