#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.0.3")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PopulateTestFiles : CliktCommand() {

  private val skipAdb by option("--skip-adb").flag(default = false)

  private val testFilesRoot = File("build/testfiles/")
  private val tempBooksRoot = File(testFilesRoot, "_generated_books")
  private val baseAudio = File("core/scanner/src/test/resources/auphonic_chapters_demo.mp3")
  private val coversRoot = File("Images/covers")

  private val folderBooks = listOf(
    BookSpec(
      id = "dream_in_a_boat",
      title = "Dream in a Boat",
      author = "Amelia Winters",
      coverDir = "Dream in a boat",
      chapters = listOf(
        ChapterSpec("Gliding into Dawn", loopCount = 4),
        ChapterSpec("Harbor Whispers", loopCount = 5),
        ChapterSpec("Nightfall Reflections", loopCount = 6),
      ),
    ),
    BookSpec(
      id = "el_guitarrero",
      title = "El Guitarrero",
      author = "Lucia Marquez",
      coverDir = "El Guitarrero",
      chapters = listOf(
        ChapterSpec("Strings Awake", loopCount = 4),
        ChapterSpec("Courtyard Echoes", loopCount = 5),
        ChapterSpec("Flamenco Midnight", loopCount = 6),
      ),
    ),
    BookSpec(
      id = "fabulous_geysha",
      title = "The Fabulous Geysha",
      author = "Lucia Marquez",
      coverDir = "The fabulous Geysha",
      chapters = listOf(
        ChapterSpec("Lantern Lights", loopCount = 4),
        ChapterSpec("Winding Garden", loopCount = 4),
        ChapterSpec("Moonlit Stage", loopCount = 5),
      ),
    ),
    BookSpec(
      id = "boy_and_the_cave",
      title = "The Boy and the Cave",
      author = "June Porter",
      coverDir = "The boy and the cave",
      chapters = listOf(
        ChapterSpec("Footsteps Below", loopCount = 3),
        ChapterSpec("Hidden Paintings", loopCount = 4),
        ChapterSpec("Echoes of Fire", loopCount = 5),
      ),
    ),
  )

  private val singleBooks = listOf(
    BookSpec(
      id = "treeman_single",
      title = "Treeman",
      author = "Amelia Winters",
      coverDir = "Treeman",
      chapters = listOf(
        ChapterSpec("Treeman", loopCount = 8),
      ),
    ),
    BookSpec(
      id = "snakey_single",
      title = "Snakey",
      author = "Harlan Pike",
      coverDir = "Snakey",
      chapters = listOf(
        ChapterSpec("Snakey", loopCount = 7),
      ),
    ),
  )

  override fun help(context: Context): String = "Populates realistic audiobook test files"

  override fun run() {
    validateEnvironment()
    cleanFolder()

    println("Preparing multi-chapter audiobooks")
    val generatedFolderBooks = generateSourceBooks(folderBooks)
    println("Preparing single-file audiobooks")
    val generatedSingleBooks = generateSourceBooks(singleBooks)

    copyIntoAudiobooksStructure(generatedFolderBooks, generatedSingleBooks)
    copyIntoSingleAudiobookStructure(generatedFolderBooks)
    copyIntoAuthorStructure(generatedFolderBooks, generatedSingleBooks)

    tempBooksRoot.deleteRecursively()

    if (!skipAdb) {
      pushToAdb()
    } else {
      println("Skipping adb push (run without --skip-adb to push to a device).")
    }
  }

  private fun validateEnvironment() {
    require(baseAudio.exists()) { "Base audio not found: ${baseAudio.absolutePath}" }
    require(coversRoot.exists()) { "Covers folder not found: ${coversRoot.absolutePath}" }
    (folderBooks + singleBooks).forEach { book ->
      require(book.coverFile().exists()) {
        "Missing cover image for ${book.title} in ${book.coverFile().parentFile?.absolutePath}"
      }
    }
  }

  private fun cleanFolder() {
    println("Cleaning output folder: ${testFilesRoot.absolutePath}")
    testFilesRoot.deleteRecursively()
    testFilesRoot.mkdirs()
    tempBooksRoot.mkdirs()
  }

  private fun generateSourceBooks(bookSpecs: List<BookSpec>): List<GeneratedBook> {
    println("Generating audiobook chapters with embedded covers (${bookSpecs.size} books)")
    return bookSpecs.map { spec ->
      val outputDir = File(tempBooksRoot, spec.directoryName()).apply { mkdirs() }
      println("- Building ${spec.title} by ${spec.author}")
      val chapters = spec.chapters.mapIndexed { index, chapter ->
        val outputFile = File(outputDir, "%02d - %s.mp3".format(index + 1, chapter.title))
        println("  • Chapter ${index + 1}/${spec.chapters.size}: ${chapter.title}")
        createChapterFile(spec, chapter, index + 1, outputFile)
        outputFile
      }
      GeneratedBook(spec = spec, chapterFiles = chapters)
    }
  }

  private fun createChapterFile(
    book: BookSpec,
    chapter: ChapterSpec,
    trackNumber: Int,
    outputFile: File,
  ) {
    val coverFile = book.coverFile()
    val metadata = listOf(
      "-metadata", "title=${chapter.title}",
      "-metadata", "album=${book.title}",
      "-metadata", "album_artist=${book.author}",
      "-metadata", "artist=${book.author}",
      "-metadata", "track=$trackNumber/${book.chapters.size}",
      "-metadata", "genre=Audiobook",
      "-metadata", "comment=Generated for Voice test library",
      "-metadata", "date=2024",
      "-metadata:s:v", "title=Cover",
      "-metadata:s:v", "comment=Cover (front)",
    )

    val command = mutableListOf(
      "ffmpeg",
      "-hide_banner",
      "-loglevel", "error",
      "-y",
      "-stream_loop", chapter.loopCount.toString(),
      "-i", baseAudio.absolutePath,
      "-i", coverFile.absolutePath,
      "-map", "0:a",
      "-map", "1:0",
      "-c:v", "copy",
      "-c:a", "libmp3lame",
      "-b:a", "160k",
      "-id3v2_version", "3",
      "-disposition:v", "attached_pic",
    )
    command.addAll(metadata)
    command.add(outputFile.absolutePath)

    runCommand(command)
  }

  private fun copyIntoAudiobooksStructure(
    generatedFolderBooks: List<GeneratedBook>,
    generatedSingleBooks: List<GeneratedBook>,
  ) {
    val audiobooksDir = File(testFilesRoot, "Audiobooks").apply { mkdirs() }
    println("Copying books into /Audiobooks structure")
    generatedFolderBooks.forEach { generatedBook ->
      val destinationDir = File(audiobooksDir, generatedBook.spec.directoryName()).apply { mkdirs() }
      generatedBook.copyChaptersTo(destinationDir)
    }

    println("Adding standalone audiobooks into /Audiobooks root")
    generatedSingleBooks.forEach { generatedBook ->
      generatedBook.chapterFiles.singleOrNull()?.let { chapter ->
        val targetFile = File(audiobooksDir, "${generatedBook.spec.title}.mp3")
        copyFile(chapter, targetFile)
      }
    }
  }

  private fun copyIntoSingleAudiobookStructure(
    generatedFolderBooks: List<GeneratedBook>,
  ) {
    val singleDir = File(testFilesRoot, "SingleAudiobook").apply { mkdirs() }
    println("Copying reference book into /SingleAudiobook structure")
    val referenceBook = generatedFolderBooks.firstOrNull() ?: return
    referenceBook.copyChaptersTo(singleDir)
  }

  private fun copyIntoAuthorStructure(
    generatedFolderBooks: List<GeneratedBook>,
    generatedSingleBooks: List<GeneratedBook>,
  ) {
    val authorsDir = File(testFilesRoot, "Authors").apply { mkdirs() }
    println("Copying books into /Authors structure")
    (generatedFolderBooks + generatedSingleBooks)
      .groupBy { it.spec.author }
      .forEach { (author, authorBooks) ->
        val authorDir = File(authorsDir, author).apply { mkdirs() }
        authorBooks.forEach { generatedBook ->
          if (generatedBook.isSingleFile()) {
            val targetFile = File(authorDir, "${generatedBook.spec.title}.mp3")
            copyFile(generatedBook.chapterFiles.single(), targetFile)
          } else {
            val destination = File(authorDir, generatedBook.spec.directoryName()).apply { mkdirs() }
            generatedBook.copyChaptersTo(destination)
          }
        }
      }
  }

  private fun GeneratedBook.copyChaptersTo(destinationDir: File) {
    destinationDir.mkdirs()
    chapterFiles.forEach { sourceFile ->
      val targetFile = File(destinationDir, sourceFile.name)
      copyFile(sourceFile, targetFile)
    }
  }

  private fun GeneratedBook.isSingleFile(): Boolean = chapterFiles.size == 1

  private fun pushToAdb() {
    println("Pushing generated audiobooks to adb")
    runCommand(listOf("adb", "shell", "rm", "-rf", "/sdcard/testfiles"))
    runCommand(listOf("adb", "push", testFilesRoot.absolutePath, "/sdcard/testfiles"))
  }

  private fun copyFile(source: File, target: File) {
    target.parentFile?.mkdirs()
    Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
  }

  private fun runCommand(command: List<String>) {
    val process = ProcessBuilder(command)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .redirectOutput(ProcessBuilder.Redirect.INHERIT)
      .start()
    val exitCode = process.waitFor()
    check(exitCode == 0) { "Command failed: ${command.joinToString(" ")}" }
  }

  private data class BookSpec(
    val id: String,
    val title: String,
    val author: String,
    val coverDir: String,
    val chapters: List<ChapterSpec>,
  ) {
    fun directoryName(): String = title
    fun coverFile(): File = File("Images/covers/$coverDir/cover.jpg")
  }

  private data class ChapterSpec(
    val title: String,
    val loopCount: Int,
  )

  private data class GeneratedBook(
    val spec: BookSpec,
    val chapterFiles: List<File>,
  )
}

PopulateTestFiles().main(args)
