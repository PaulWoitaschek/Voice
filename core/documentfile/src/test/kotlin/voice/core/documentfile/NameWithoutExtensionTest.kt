package voice.core.documentfile

import org.junit.Rule
import org.junit.rules.TemporaryFolder
import kotlin.test.Test
import kotlin.test.assertEquals

class NameWithoutExtensionTest {

  @get:Rule
  val testFolder = TemporaryFolder()

  @Test
  fun keepsDotsForDirectoryNames() {
    val folder = testFolder.newFolder("Author.Name")

    assertEquals(expected = "Author.Name", actual = FileBasedDocumentFile(folder).nameWithoutExtension())
  }

  @Test
  fun stripsExtensionForFileNames() {
    val file = testFolder.newFile("Chapter.01.m4b")

    assertEquals(expected = "Chapter.01", actual = FileBasedDocumentFile(file).nameWithoutExtension())
  }
}
