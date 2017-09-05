package de.ph1b.audiobook.chapterreader

import dagger.Reusable
import de.ph1b.audiobook.chapterreader.id3.ID3ChapterReader
import de.ph1b.audiobook.chapterreader.matroska.MatroskaChapterReader
import de.ph1b.audiobook.chapterreader.mp4.Mp4ChapterReader
import de.ph1b.audiobook.chapterreader.ogg.OggChapterReader
import java.io.File
import javax.inject.Inject

@Reusable
class ChapterReader @Inject internal constructor(
    private val oggReader: OggChapterReader,
    private val mp4Reader: Mp4ChapterReader,
    private val matroskaReader: MatroskaChapterReader,
    private val id3Reader: ID3ChapterReader
) {

  fun read(file: File): Map<Int, String> = when (file.extension) {
    "mp3" -> id3Reader.read(file)
    "mp4", "m4a", "m4b", "aac" -> mp4Reader.readChapters(file)
    "opus", "ogg", "oga" -> oggReader.read(file)
    "mka", "mkv", "webm" -> matroskaReader.read(file)
    else -> emptyMap()
  }
}
