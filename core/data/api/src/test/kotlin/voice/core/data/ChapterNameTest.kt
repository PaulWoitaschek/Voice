package voice.core.data

import kotlin.test.Test
import kotlin.test.assertEquals

class ChapterNameTest {

  @Test
  fun `a single chapter has no name`() {
    assertEquals(expected = null, actual = name("Echoes of Tomorrow", number = 1, chapterCount = 1))
  }

  @Test
  fun `names without letters are dropped`() {
    assertEquals(expected = null, actual = name("001", number = 1))
    assertEquals(expected = null, actual = name("01", number = 16))
    assertEquals(expected = null, actual = name("  "))
    assertEquals(expected = null, actual = name(null))
  }

  @Test
  fun `file extensions are stripped`() {
    assertEquals(expected = "The Signal", actual = name("The Signal.mp3"))
    assertEquals(expected = null, actual = name("003.m4a", number = 3))
  }

  @Test
  fun `a generic name with the chapter number is dropped`() {
    listOf("Chapter 3", "Kapitel 03", "Track_3", "CD3", "3. Kapitel", "Глава 3", "Kapitel")
      .forEach {
        assertEquals(expected = null, actual = name(it, number = 3))
      }
  }

  @Test
  fun `a generic name with another number stays`() {
    assertEquals(expected = "Kapitel 1", actual = name("Kapitel 1", number = 2))
    assertEquals(expected = "Track 03", actual = name("Track 03", number = 5))
  }

  @Test
  fun `a track number that matches the chapter number is stripped`() {
    assertEquals(expected = "The Signal", actual = name("03 - The Signal", number = 3))
    assertEquals(expected = "Static", actual = name("3_Static.m4a", number = 3))
    assertEquals(expected = "The Lighthouse", actual = name("0007 The Lighthouse", number = 7))
    assertEquals(expected = null, actual = name("01 - Kapitel 1", number = 1))
  }

  @Test
  fun `a track number that differs from the chapter number stays`() {
    assertEquals(expected = "03 - The Signal", actual = name("03 - The Signal", number = 4))
  }

  @Test
  fun `a number that may be part of the title stays`() {
    assertEquals(expected = "1 Mose", actual = name("1 Mose", number = 1))
    assertEquals(expected = "1. Akt", actual = name("1. Akt", number = 1))
    assertEquals(expected = "3 Musketiere", actual = name("3 Musketiere", number = 5))
  }

  @Test
  fun `names with more than a generic word stay`() {
    assertEquals(
      expected = "Folge 63: Monster-Wolken",
      actual = name("Folge 63: Monster-Wolken", number = 60),
    )
    assertEquals(expected = "Teil 2, Kapitel 3", actual = name("Teil 2, Kapitel 3", number = 14))
    assertEquals(expected = "Chapter One", actual = name("Chapter One", number = 1))
  }

  private fun name(
    name: String?,
    number: Int = 1,
    chapterCount: Int = 20,
  ): String? = ChapterMark(name = name, startMs = 0, endMs = 1).displayName(number, chapterCount)
}
