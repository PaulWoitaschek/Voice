package de.paulwoitaschek.chapterreader.di

import dagger.Component
import de.paulwoitaschek.chapterreader.ChapterReader

@Component
internal interface ChapterReaderComponent {

  fun chapterReader(): ChapterReader
}
