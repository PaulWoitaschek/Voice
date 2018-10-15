package de.ph1b.audiobook.data

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout

class BookSubject private constructor(failureMetaData: FailureMetadata, actual: Book?) :
  Subject<BookSubject, Book?>(failureMetaData, actual) {

  fun positionIs(position: Int) {
    if (actual()?.content?.position != position) {
      failWithActual("position", position)
    }
  }

  fun durationIs(duration: Int) {
    if (actual()?.content?.duration != duration) {
      failWithActual("duration", duration)
    }
  }

  fun currentChapterIs(currentChapter: Chapter) {
    if (actual()?.content?.currentChapter != currentChapter) {
      failWithActual("currentChapter", currentChapter)
    }
  }

  fun currentChapterIndexIs(currentChapterIndex: Int) {
    if (actual()?.content?.currentChapterIndex != currentChapterIndex) {
      failWithActual("currentChapterIndex", currentChapterIndex)
    }
  }

  fun nextChapterIs(nextChapter: Chapter?) {
    if (actual()?.content?.nextChapter != nextChapter) {
      failWithActual("nextChapter", arrayOf(nextChapter))
    }
  }

  fun previousChapterIs(previousChapter: Chapter?) {
    if (actual()?.content?.previousChapter != previousChapter) {
      failWithActual("previousChapter", arrayOf(previousChapter))
    }
  }

  companion object {
    val factory = Factory(::BookSubject)
  }
}

fun Book?.assertThat(): BookSubject = assertAbout(BookSubject.factory).that(this)
