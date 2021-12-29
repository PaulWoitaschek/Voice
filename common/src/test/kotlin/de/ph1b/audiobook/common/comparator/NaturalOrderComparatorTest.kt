package de.ph1b.audiobook.common.comparator

import android.net.Uri
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import java.io.File

@RunWith(AndroidJUnit4::class)
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
    val expected = testFiles()
    val sorted = expected.sortedWith(NaturalOrderComparator.fileComparator)
    assertThat(sorted).isEqualTo(expected)
  }

  private fun testFiles(): List<File> {
    testFolder.newFolder("folder", "subfolder", "subsubfolder")
    testFolder.newFolder("storage", "emulated", "0")
    testFolder.newFolder("xFolder")

    return listOf(
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
      "e",
      "folder1/1.mp3",
      "folder1/10.mp3",
      "folder2/2.mp3",
      "folder10/1.mp3",
    )
    val sorted = expected.sortedWith(NaturalOrderComparator.stringComparator)
    assertThat(sorted).isEqualTo(expected)
  }

  @Test
  fun uriComparatorContent() {
    val expected = listOf(
      "folder1/1.mp3",
      "folder1/10.mp3",
      "folder2/2.mp3",
      "folder10/1.mp3",
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
      "e",
    )

    val uris = expected.map {
      Uri.Builder()
        .scheme("content")
        .authority("com.android.externalstorage.documents")
        .appendPath("tree")
        .appendPath("primary:audiobooks")
        .appendPath("document")
        .appendPath("primary:audiobooks/$it")
        .build()
    }

    assertThat(uris.sortedWith(NaturalOrderComparator.uriComparator))
      .containsExactlyElementsIn(uris)
      .inOrder()
  }

  @Test
  fun uriComparatorFiles() {
    val expected = testFiles()
    val uris = expected.map { Uri.fromFile(it) }

    assertThat(uris.sortedWith(NaturalOrderComparator.uriComparator))
      .containsExactlyElementsIn(uris)
      .inOrder()
  }


  @After
  fun tearDown() {
    testFolder.delete()
  }
}
