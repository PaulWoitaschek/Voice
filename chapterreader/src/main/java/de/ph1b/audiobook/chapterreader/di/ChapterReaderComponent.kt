package de.ph1b.audiobook.chapterreader.di

import dagger.BindsInstance
import dagger.Component
import de.ph1b.audiobook.chapterreader.ChapterReader
import de.ph1b.audiobook.common.ErrorReporter
import de.ph1b.audiobook.common.Logger

@Component
internal interface ChapterReaderComponent {

  fun chapterReader(): ChapterReader

  @Component.Builder
  interface Builder {

    @BindsInstance
    fun logger(logger: Logger): Builder

    @BindsInstance
    fun errorReporter(errorReporter: ErrorReporter): Builder

    fun build(): ChapterReaderComponent
  }
}
