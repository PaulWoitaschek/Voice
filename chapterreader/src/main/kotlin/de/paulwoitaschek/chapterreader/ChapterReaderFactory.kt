package de.paulwoitaschek.chapterreader

import de.paulwoitaschek.chapterreader.id3.ID3ChapterReader
import de.paulwoitaschek.chapterreader.matroska.MatroskaChapterReader
import de.paulwoitaschek.chapterreader.matroska.ReadAsMatroskaChapters
import de.paulwoitaschek.chapterreader.mp4.Mp4ChapterReader
import de.paulwoitaschek.chapterreader.ogg.OggChapterReader

/**
 * Factory class for creating the chapter reader
 */
object ChapterReaderFactory {

  /**
   * Creates a new [ChapterReader].
   *
   * @return The created chapter reader
   */
  @JvmStatic
  fun create(): ChapterReader = ChapterReader(
    oggReader = OggChapterReader(),
    mp4Reader = Mp4ChapterReader(),
    id3Reader = ID3ChapterReader(),
    matroskaReader = MatroskaChapterReader(
      ReadAsMatroskaChapters(),
    ),
  )
}
