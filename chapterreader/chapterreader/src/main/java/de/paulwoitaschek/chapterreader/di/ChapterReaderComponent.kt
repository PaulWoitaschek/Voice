package de.paulwoitaschek.chapterreader.di

import dagger.BindsInstance
import dagger.Component
import de.paulwoitaschek.chapterreader.ChapterReader
import de.paulwoitaschek.chapterreader.misc.Logger

@Component
internal interface ChapterReaderComponent {

  fun chapterReader(): ChapterReader

  @Component.Builder
  interface Builder {

    @BindsInstance
    fun logger(logger: Logger): Builder

    fun build(): ChapterReaderComponent
  }
}
