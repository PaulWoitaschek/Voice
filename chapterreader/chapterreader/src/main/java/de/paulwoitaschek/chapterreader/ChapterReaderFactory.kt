package de.paulwoitaschek.chapterreader

import de.paulwoitaschek.chapterreader.di.DaggerChapterReaderComponent

/**
 * Factory class for creating the chapter reader
 */
object ChapterReaderFactory {

  /**
   * Creates a new [ChapterReader].
   *
   * @return The created chapter reader
   */
  fun create(): ChapterReader =
    DaggerChapterReaderComponent.builder()
      .build()
      .chapterReader()
}
