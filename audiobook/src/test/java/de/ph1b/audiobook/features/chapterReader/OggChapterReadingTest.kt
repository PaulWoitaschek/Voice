package de.ph1b.audiobook.features.chapterReader

import de.ph1b.audiobook.misc.toMap
import org.assertj.core.api.Assertions.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class OggChapterReadingTest {
  @Test
  fun readChaptersFromOggOpusTest() {
    val simpleOpusResource = javaClass.classLoader.getResource("oggChapterReader/simple.opus")
    val chapters = File(simpleOpusResource.path).inputStream().use {
      readChaptersFromOgg(it)
    }.toMap()

    assertThat(chapters).isEqualTo(mapOf(
        0 to "Chapter 1",
        1000 to "Chapter 2",
        2000 to "Chapter 3",
        3000 to "Chapter 4"
    ))
  }
}
