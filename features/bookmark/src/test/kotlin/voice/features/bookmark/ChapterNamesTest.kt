package voice.features.bookmark

import kotlin.test.Test
import kotlin.test.assertEquals

class ChapterNamesTest {

  @Test
  fun `a single chapter has no name`() {
    assertEquals(expected = listOf(null), actual = meaningfulChapterNames(listOf("Echoes of Tomorrow")))
  }

  @Test
  fun `track numbers and file extensions are stripped`() {
    assertEquals(
      expected = listOf("The Signal", "Static"),
      actual = meaningfulChapterNames(listOf("01 - The Signal.mp3", "02_Static.m4a")),
    )
  }

  @Test
  fun `numbered generic names are dropped`() {
    assertEquals(
      expected = listOf(null, null, null),
      actual = meaningfulChapterNames(listOf("Chapter 1", "Chapter 2", "Track 03")),
    )
  }

  @Test
  fun `a file name repeated with a running number is dropped`() {
    assertEquals(
      expected = listOf("Prologue", null, null),
      actual = meaningfulChapterNames(listOf("Prologue", "Echoes of Tomorrow 01", "Echoes of Tomorrow 02")),
    )
  }

  @Test
  fun `names without letters are dropped`() {
    assertEquals(expected = listOf(null, "Mira"), actual = meaningfulChapterNames(listOf("001", "Mira")))
  }
}
