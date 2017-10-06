package de.ph1b.audiobook.chapterreader

import de.ph1b.audiobook.chapterreader.di.DaggerChapterReaderComponent
import de.ph1b.audiobook.common.ErrorReporter
import de.ph1b.audiobook.common.Logger

object ChapterReaderFactory {

  fun create(logger: Logger, errorReporter: ErrorReporter): ChapterReader =
      DaggerChapterReaderComponent.builder()
          .logger(logger)
          .errorReporter(errorReporter)
          .build()
          .chapterReader()
}
