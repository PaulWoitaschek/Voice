package de.paulwoitaschek.chapterreader.mp4

import com.google.common.truth.Truth.assertThat
import de.paulwoitaschek.chapterreader.Chapter
import org.junit.jupiter.api.Test
import java.io.File

class ChapReaderTest {

  @Test
  fun parse() {
    val file = File(javaClass.classLoader.getResource("mp4/test.m4b").file)
    val actual = ChapReader.read(file)

    assertThat(actual).containsExactly(
      Chapter(0, "01"),
      Chapter(2193, "02"),
      Chapter(4517, "03"),
      Chapter(6838, "04"),
      Chapter(9580, "05"),
      Chapter(12270, "06"),
      Chapter(14855, "07"),
      Chapter(17545, "08"),
      Chapter(20182, "09"),
      Chapter(22505, "10")
    )
  }
}
