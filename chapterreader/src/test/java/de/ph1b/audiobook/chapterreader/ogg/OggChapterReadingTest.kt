package de.ph1b.audiobook.chapterreader.ogg

import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.chapterreader.NoOpLogger
import org.junit.Before
import org.junit.Test
import java.io.File

class OggChapterReadingTest {

  private lateinit var oggChapterReader: OggChapterReader

  @Before
  fun setUp() {
    oggChapterReader = OggChapterReader(NoOpLogger)
  }

  @Test
  fun readChaptersFromOggOpusTest() {
    val file = File(javaClass.classLoader.getResource("ogg/simple.opus").file)
    val chapters = oggChapterReader.read(file)

    assertThat(chapters).isEqualTo(
        mapOf(
            0 to "Chapter 1",
            1000 to "Chapter 2",
            2000 to "Chapter 3",
            3000 to "Chapter 4"
        )
    )
  }

  @Test
  fun readChaptersFromOggVorbisTest() {
    val file = File(javaClass.classLoader.getResource("ogg/simple_vorbis.ogg").file)
    val chapters = oggChapterReader.read(file)

    assertThat(chapters).isEqualTo(
        mapOf(
            0 to "Part 1",
            20 to "Part 2",
            2000 to "Part 3"
        )
    )
  }
}
