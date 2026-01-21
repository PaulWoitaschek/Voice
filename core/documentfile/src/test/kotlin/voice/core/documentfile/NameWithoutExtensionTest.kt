package voice.core.documentfile

import io.kotest.matchers.shouldBe
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

class NameWithoutExtensionTest {

  @get:Rule
  val testFolder = TemporaryFolder()

  @Test
  fun keepsDotsForDirectoryNames() {
    val folder = testFolder.newFolder("Author.Name")

    FileBasedDocumentFile(folder).nameWithoutExtension() shouldBe "Author.Name"
  }

  @Test
  fun stripsExtensionForFileNames() {
    val file = testFolder.newFile("Chapter.01.m4b")

    FileBasedDocumentFile(file).nameWithoutExtension() shouldBe "Chapter.01"
  }
}
