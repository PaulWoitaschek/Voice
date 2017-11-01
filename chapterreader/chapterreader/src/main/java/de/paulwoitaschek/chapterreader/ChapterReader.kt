package de.paulwoitaschek.chapterreader


import de.paulwoitaschek.chapterreader.id3.ID3ChapterReader
import de.paulwoitaschek.chapterreader.matroska.MatroskaChapterReader
import de.paulwoitaschek.chapterreader.mp4.Mp4ChapterReader
import de.paulwoitaschek.chapterreader.ogg.OggChapterReader
import java.io.File
import javax.inject.Inject

/**
 * A chapter reader which reads the music chapters of a file.
 */
class ChapterReader @Inject internal constructor(
  private val oggReader: OggChapterReader,
  private val mp4Reader: Mp4ChapterReader,
  private val matroskaReader: MatroskaChapterReader,
  private val id3Reader: ID3ChapterReader
) {

  /**
   * Read the chapters for a file. The result is sorted by the start of the chapters.
   *
   * @param file the file to read
   * @return the parsed chapters
   */
  fun read(file: File): List<Chapter> {
    val chapters = when (file.extension) {
      "mp3" -> id3Reader.read(file)
      "mp4", "m4a", "m4b", "aac" -> mp4Reader.readChapters(file)
      "opus", "ogg", "oga" -> oggReader.read(file)
      "mka", "mkv", "webm" -> matroskaReader.read(file)
      else -> emptyList()
    }
    return chapters.sorted()
  }
}
