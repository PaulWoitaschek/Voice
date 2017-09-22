package de.ph1b.audiobook.chapterreader

import de.ph1b.audiobook.chapterreader.di.ChapterReaderComponent
import de.ph1b.audiobook.chapterreader.di.DaggerChapterReaderComponent
import de.ph1b.audiobook.common.ErrorReporter
import de.ph1b.audiobook.common.Logger

object ChapterReaderFactory {

  private var component: ChapterReaderComponent? = null

  fun create(logger: Logger, errorReporter: ErrorReporter): ChapterReader {
    if (component == null) {
      component = DaggerChapterReaderComponent.builder()
          .logger(logger)
          .errorReporter(errorReporter)
          .build()
    }
    return component!!.chapterReader()
  }
}
