package de.paulwoitaschek.chapterreader

import de.paulwoitaschek.chapterreader.di.DaggerChapterReaderComponent
import de.paulwoitaschek.chapterreader.misc.Logger

object ChapterReaderFactory {

  fun create(logger: Logger): ChapterReader =
    DaggerChapterReaderComponent.builder()
      .logger(logger)
      .build()
      .chapterReader()
}
