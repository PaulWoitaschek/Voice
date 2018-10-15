package de.ph1b.audiobook.common.comparator

import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NaturalOrderComparatorTest {

  private val testFolder = TemporaryFolder()

  @Rule
  fun testFolder() = testFolder

  @Before
  fun setUp() {
    testFolder.create()
  }

  @Test
  fun fileComparator() {
    testFolder.newFolder("folder", "subfolder", "subsubfolder")
    testFolder.newFolder("storage", "emulated", "0")
    testFolder.newFolder("xFolder")

    val alarmsFolder = testFolder.newFolder("storage", "emulated", "0", "Alarms")
    val expected = listOf(
      alarmsFolder,
      testFolder.newFile("folder/subfolder/subsubfolder/test2.mp3"),
      testFolder.newFile("folder/subfolder/test.mp3"),
      testFolder.newFile("folder/subfolder/test2.mp3"),
      testFolder.newFile("folder/a.jpg"),
      testFolder.newFile("folder/aC.jpg"),
      testFolder.newFile("storage/emulated/0/1.ogg"),
      testFolder.newFile("storage/emulated/0/2.ogg"),
      testFolder.newFile("xFolder/d.jpg"),
      testFolder.newFile("1.mp3"),
      testFolder.newFile("a.jpg")
    )

    val sorted = expected.sortedWith(NaturalOrderComparator.fileComparator)
    assertThat(sorted).isEqualTo(expected)
  }

  @Test
  fun stringComparator() {
    val expected = listOf(
      "00 I",
      "00 Introduction",
      "1",
      "01 How to build a universe",
      "01 I",
      "2",
      "9",
      "10",
      "a",
      "Ab",
      "aC",
      "Ba",
      "cA",
      "D",
      "e"
    )

    val sorted = expected.sortedWith(NaturalOrderComparator.stringComparator)
    assertThat(sorted).isEqualTo(expected)
  }

  @After
  fun tearDown() {
    testFolder.delete()
  }
}
