#!/usr/bin/env kotlin
@file:DependsOn("com.github.ajalt.clikt:clikt-jvm:5.1.0")

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.Context
import com.github.ajalt.clikt.core.main
import com.github.ajalt.clikt.parameters.options.flag
import com.github.ajalt.clikt.parameters.options.option
import java.io.File

class PopulateTestFiles : CliktCommand() {

  private val skipAdb by option("--skip-adb").flag(default = false)

  private val testFilesRoot = File("build/testfiles/")
  private val workDir = File("build/testfiles-work/")
  private val samplesRoot = File("core/scanner/src/test/resources")
  private val demoArtworksRoot = File("artwork/demo-data")

  private val books = listOf(
    BookSpec(
      title = "Dream in a Boat",
      author = "Amelia Winters",
      artwork = "horizon.webp",
      format = Format.Mp3,
      chapters = listOf(
        ChapterSpec("Gliding into Dawn", loopCount = 4),
        ChapterSpec("Harbor Whispers", loopCount = 5),
        ChapterSpec("Nightfall Reflections", loopCount = 6),
      ),
    ),
    BookSpec(
      title = "El Guitarrero",
      author = "Lucia Marquez",
      artwork = "bookshop.webp",
      format = Format.Mp3,
      chapters = listOf(
        ChapterSpec("Strings Awake", loopCount = 4),
        ChapterSpec("Courtyard Echoes", loopCount = 5),
        ChapterSpec("Flamenco Midnight", loopCount = 6),
      ),
    ),
    BookSpec(
      title = "The Fabulous Geysha",
      author = "Lucia Marquez",
      artwork = "echoes.webp",
      format = Format.M4b,
      chapters = listOf(
        ChapterSpec("Lantern Lights", loopCount = 4),
        ChapterSpec("Winding Garden", loopCount = 4),
        ChapterSpec("Moonlit Stage", loopCount = 5),
      ),
    ),
    BookSpec(
      title = "The Boy and the Cave",
      author = "June Porter",
      artwork = "horizon.webp",
      format = Format.Opus,
      chapters = listOf(
        ChapterSpec("Footsteps Below", loopCount = 3),
        ChapterSpec("Hidden Paintings", loopCount = 4),
        ChapterSpec("Echoes of Fire", loopCount = 5),
      ),
    ),
    BookSpec(
      title = "Treeman",
      author = "Amelia Winters",
      artwork = "bookshop.webp",
      format = Format.Mp3,
      chapters = listOf(
        ChapterSpec("Treeman", loopCount = 8),
      ),
    ),
    BookSpec(
      title = "Snakey",
      author = "Harlan Pike",
      artwork = "echoes.webp",
      format = Format.Mp3,
      chapters = listOf(
        ChapterSpec("Snakey", loopCount = 7),
      ),
    ),
  )

  override fun help(context: Context): String = "Populates realistic audiobook test files: MP3 folders, an M4B with chapters, and Opus"

  override fun run() {
    validateEnvironment()
    testFilesRoot.deleteRecursively()
    workDir.deleteRecursively()
    workDir.mkdirs()

    // Chapters loop these with stream copy instead of re-encoding, which keeps the generation instant.
    val samples = mapOf(
      Format.Mp3 to encodeMp3Sample(),
      Format.M4b to File(samplesRoot, "auphonic_chapters_demo.m4a"),
      Format.Opus to File(samplesRoot, "auphonic_chapters_demo.opus"),
    )
    println("Generating ${books.size} audiobooks")
    val generatedBooks = books.map { book ->
      println("- ${book.title} by ${book.author} (${book.format})")
      GeneratedBook(book, generate(book, samples.getValue(book.format)))
    }

    copyIntoAudiobooksStructure(generatedBooks)
    copyIntoSingleAudiobookStructure(generatedBooks.first())
    copyIntoAuthorStructure(generatedBooks)
    workDir.deleteRecursively()

    if (!skipAdb) {
      pushToAdb()
    } else {
      println("Skipping adb push (run without --skip-adb to push to a device).")
    }
  }

  private fun validateEnvironment() {
    require(demoArtworksRoot.exists()) { "Demo artworks folder not found: ${demoArtworksRoot.absolutePath}" }
    books.forEach { book ->
      require(book.coverFile().exists()) {
        "Missing demo artwork for ${book.title}: ${book.coverFile().absolutePath}"
      }
    }
  }

  private fun encodeMp3Sample(): File {
    val sample = File(workDir, "sample.mp3")
    ffmpeg(
      "-i", File(samplesRoot, "auphonic_chapters_demo.mp3").absolutePath,
      "-map", "0:a",
      "-ac", "1",
      "-c:a", "libmp3lame",
      "-b:a", "64k",
      sample.absolutePath,
    )
    return sample
  }

  private fun generate(
    book: BookSpec,
    sample: File,
  ): List<File> {
    val outputDir = File(workDir, book.title).apply { mkdirs() }
    return when (book.format) {
      Format.Mp3 -> book.chapters.mapIndexed { index, chapter ->
        val outputFile = File(outputDir, book.chapterFileName(index))
        ffmpeg(
          "-stream_loop", chapter.loopCount.toString(),
          "-i", sample.absolutePath,
          "-i", book.coverFile().absolutePath,
          "-map", "0:a",
          "-map", "1:0",
          "-c:a", "copy",
          "-c:v", "mjpeg",
          "-disposition:v", "attached_pic",
          "-id3v2_version", "3",
          "-metadata:s:v", "title=Cover",
          "-metadata:s:v", "comment=Cover (front)",
          "-map_metadata", "-1",
          *book.metadata(chapter.title, trackNumber = index + 1),
          outputFile.absolutePath,
        )
        outputFile
      }
      Format.Opus -> {
        val chapterFiles = book.chapters.mapIndexed { index, chapter ->
          val outputFile = File(outputDir, book.chapterFileName(index))
          ffmpeg(
            "-stream_loop", chapter.loopCount.toString(),
            "-i", sample.absolutePath,
            "-map", "0:a",
            "-map_chapters", "-1",
            "-map_metadata", "-1",
            "-c:a", "copy",
            *book.metadata(chapter.title, trackNumber = index + 1),
            outputFile.absolutePath,
          )
          outputFile
        }
        // tests covers that are found next to the files
        val cover = File(outputDir, "cover.jpg")
        ffmpeg("-i", book.coverFile().absolutePath, cover.absolutePath)
        chapterFiles + cover
      }
      Format.M4b -> {
        val sampleMs = (audioDurationSeconds(sample) * 1000).toLong()
        var startMs = 0L
        val chapterMetadata = buildString {
          appendLine(";FFMETADATA1")
          book.chapters.forEach { chapter ->
            val endMs = startMs + (chapter.loopCount + 1) * sampleMs
            appendLine("[CHAPTER]")
            appendLine("TIMEBASE=1/1000")
            appendLine("START=$startMs")
            appendLine("END=$endMs")
            appendLine("title=${chapter.title.escapeFfmetadata()}")
            startMs = endMs
          }
        }
        val chaptersFile = File(workDir, "${book.title}.chapters.txt").apply { writeText(chapterMetadata) }
        val outputFile = File(outputDir, "${book.title}.${book.format.extension}")
        ffmpeg(
          "-stream_loop", (book.chapters.sumOf { it.loopCount + 1 } - 1).toString(),
          "-i", sample.absolutePath,
          "-i", book.coverFile().absolutePath,
          "-i", chaptersFile.absolutePath,
          "-map", "0:a",
          "-map", "1:0",
          // the chapter titles come along with the metadata of the chapters file
          "-map_metadata", "2",
          "-map_chapters", "2",
          "-c:a", "copy",
          "-c:v", "mjpeg",
          "-disposition:v", "attached_pic",
          *book.metadata(book.title, trackNumber = null),
          outputFile.absolutePath,
        )
        listOf(outputFile)
      }
    }
  }

  private fun copyIntoAudiobooksStructure(generatedBooks: List<GeneratedBook>) {
    println("Copying books into /Audiobooks structure")
    val audiobooksDir = File(testFilesRoot, "Audiobooks")
    generatedBooks.forEach { it.copyInto(audiobooksDir) }
  }

  private fun copyIntoSingleAudiobookStructure(referenceBook: GeneratedBook) {
    println("Copying ${referenceBook.spec.title} into /SingleAudiobook structure")
    val singleDir = File(testFilesRoot, "SingleAudiobook")
    referenceBook.files.forEach { it.copyTo(File(singleDir, it.name)) }
  }

  private fun copyIntoAuthorStructure(generatedBooks: List<GeneratedBook>) {
    println("Copying books into /Authors structure")
    generatedBooks
      .groupBy { it.spec.author }
      .forEach { (author, authorBooks) ->
        val authorDir = File(testFilesRoot, "Authors/$author")
        authorBooks.forEach { it.copyInto(authorDir) }
      }
  }

  private fun GeneratedBook.copyInto(parent: File) {
    val single = files.singleOrNull()
    if (single != null) {
      single.copyTo(File(parent, "${spec.title}.${single.extension}"))
    } else {
      files.forEach { it.copyTo(File(parent, "${spec.title}/${it.name}")) }
    }
  }

  private fun pushToAdb() {
    println("Pushing generated audiobooks to adb")
    runCommand(listOf("adb", "shell", "rm", "-rf", "/sdcard/testfiles"))
    runCommand(listOf("adb", "push", testFilesRoot.absolutePath, "/sdcard/testfiles"))
  }

  private fun audioDurationSeconds(file: File): Double {
    val process = ProcessBuilder(
      "ffprobe", "-v", "error", "-select_streams", "a:0", "-show_entries", "stream=duration", "-of", "csv=p=0", file.absolutePath,
    ).start()
    val output = process.inputStream.bufferedReader().readText().trim()
    check(process.waitFor() == 0) { "ffprobe failed for $file" }
    return output.toDouble()
  }

  private fun String.escapeFfmetadata(): String = replace(Regex("[=;#\\\\\n]")) { "\\" + it.value }

  private fun ffmpeg(vararg arguments: String) {
    runCommand(listOf("ffmpeg", "-hide_banner", "-loglevel", "error", "-y") + arguments)
  }

  private fun runCommand(command: List<String>) {
    val process = ProcessBuilder(command)
      .redirectError(ProcessBuilder.Redirect.INHERIT)
      .redirectOutput(ProcessBuilder.Redirect.INHERIT)
      .start()
    val exitCode = process.waitFor()
    check(exitCode == 0) { "Command failed: ${command.joinToString(" ")}" }
  }

  private enum class Format(val extension: String) {
    Mp3("mp3"),
    M4b("m4b"),
    Opus("opus"),
  }

  private data class BookSpec(
    val title: String,
    val author: String,
    val artwork: String,
    val format: Format,
    val chapters: List<ChapterSpec>,
  ) {
    fun coverFile(): File = File("artwork/demo-data/$artwork")

    fun chapterFileName(index: Int): String {
      return if (chapters.size == 1) {
        "$title.${format.extension}"
      } else {
        "%02d - %s.%s".format(index + 1, chapters[index].title, format.extension)
      }
    }

    fun metadata(
      title: String,
      trackNumber: Int?,
    ): Array<String> {
      return listOfNotNull(
        "-metadata" to "title=$title",
        "-metadata" to "album=${this.title}",
        "-metadata" to "album_artist=$author",
        "-metadata" to "artist=$author",
        trackNumber?.let { "-metadata" to "track=$it/${chapters.size}" },
        "-metadata" to "genre=Audiobook",
        "-metadata" to "comment=Generated for Voice test library",
        "-metadata" to "date=2024",
      ).flatMap { listOf(it.first, it.second) }.toTypedArray()
    }
  }

  private data class ChapterSpec(
    val title: String,
    val loopCount: Int,
  )

  private data class GeneratedBook(
    val spec: BookSpec,
    val files: List<File>,
  )
}

PopulateTestFiles().main(args)
