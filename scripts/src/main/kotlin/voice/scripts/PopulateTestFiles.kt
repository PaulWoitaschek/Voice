package voice.scripts

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import java.io.File

class PopulateTestFiles : CliktCommand() {

  private val testFilesRoot = File("build/testfiles/")

  override fun run() {
    cleanFolder()

    addRootBooks()
    addSingleBook()
    addAuthorBooks()

    pushToAdb()
  }

  override fun help(context: Context): String = "Populates test files using adb"

  private fun pushToAdb() {
    println("Pushing to adb")
    runCommand("adb", "shell", "rm", "-rf", "/sdcard/testfiles")
    runCommand("adb", "push", testFilesRoot.absolutePath, "/sdcard/testfiles")
  }

  private fun addAuthorBooks() {
    add(path = "Authors/Author1/Book1/1.mp3")
    add(path = "Authors/Author1/Book1/2.mp3")

    add(path = "Authors/Author1/Book2/1.mp3")
    add(path = "Authors/Author1/Book2/2.mp3")

    add(path = "Authors/Author1/Book3/2.mp3")

    add(path = "Authors/Book4.mp3")

    add(path = "Authors/Author1/Book5.mp3")

    add(path = "Authors/Author2/Book6/1.mp3")
  }

  private fun addSingleBook() {
    add(path = "SingleAudiobook/1.mp3")
    add(path = "SingleAudiobook/2.mp3")
  }

  private fun addRootBooks() {
    add(path = "root/Book1.mp3")

    add(path = "root/Book2.mp3")

    add(path = "root/Book3/1.mp3")
    add(path = "root/Book3/2.mp3")

    add(path = "root/Book4/1.mp3")
    add(path = "root/Book4/2.mp3")
    add(path = "root/Book4/3.mp3")
  }

  private fun cleanFolder() {
    println("Cleaning folder")
    testFilesRoot.deleteRecursively()
    testFilesRoot.mkdirs()
  }

  private fun add(path: String) {
    val testFile = File("app/src/androidTest/res/raw/intact.mp3")
    val output = File(testFilesRoot, path)
    output.parentFile.mkdirs()
    runCommand(
      "ffmpeg",
      "-i",
      testFile.absolutePath,
      "-c",
      "copy",
      "-metadata",
      "album=${output.nameWithoutExtension}",
      output.absolutePath,
    )
  }
}
