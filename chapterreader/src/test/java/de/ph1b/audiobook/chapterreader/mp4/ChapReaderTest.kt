package de.ph1b.audiobook.chapterreader.mp4

import org.assertj.core.api.Assertions.assertThat
import org.junit.Test
import java.io.File

class ChapReaderTest {

  @Test
  fun parse() {
    val file = File(javaClass.classLoader.getResource("mp4/test.m4b").file)
    val actual = ChapReader.read(file)
        .toSortedMap()
        .toList()
        .sortedBy { it.first }

    assertThat(actual).containsExactly(
        0 to "01",
        1316 to "02",
        2710 to "03",
        4103 to "04",
        5748 to "05",
        7362 to "06",
        8913 to "07",
        10527 to "08",
        12109 to "09",
        13503 to "10"
    )
  }
}
