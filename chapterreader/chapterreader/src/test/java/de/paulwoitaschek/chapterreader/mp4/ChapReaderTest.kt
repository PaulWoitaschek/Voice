package de.paulwoitaschek.chapterreader.mp4

import com.google.common.truth.Truth.assertThat
import de.paulwoitaschek.chapterreader.Chapter
import org.junit.Test
import java.io.File

class ChapReaderTest {

  @Test
  fun parse() {
    val file = File(javaClass.classLoader.getResource("mp4/test.m4b").file)
    val actual = ChapReader.read(file)

    assertThat(actual).containsExactly(
      Chapter(0L, "01"),
      Chapter(1316L, "02"),
      Chapter(2710L, "03"),
      Chapter(4103L, "04"),
      Chapter(5748L, "05"),
      Chapter(7362L, "06"),
      Chapter(8913L, "07"),
      Chapter(10527L, "08"),
      Chapter(12109L, "09"),
      Chapter(13503L, "10")
    )
  }
}
