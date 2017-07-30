package de.ph1b.audiobook.chapterreader.ogg

import de.ph1b.audiobook.chapterreader.matroska.NoOpLogger
import org.assertj.core.api.Assertions.assertThat
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
    val simpleOpusResource = javaClass.classLoader.getResource("ogg/simple.opus")
    val chapters = File(simpleOpusResource.path).inputStream().use {
      oggChapterReader.read(it)
    }

    assertThat(chapters).isEqualTo(mapOf(
        0 to "Chapter 1",
        1000 to "Chapter 2",
        2000 to "Chapter 3",
        3000 to "Chapter 4"
    ))
  }

  @Test
  fun readChaptersFromOggVorbisTest() {
    val simpleOpusResource = javaClass.classLoader.getResource("ogg/simple_vorbis.ogg")
    val chapters = File(simpleOpusResource.path).inputStream().use {
      oggChapterReader.read(it)
    }

    assertThat(chapters).isEqualTo(mapOf(
        0 to "Part 1",
        20 to "Part 2",
        2000 to "Part 3"
    ))
  }
}
