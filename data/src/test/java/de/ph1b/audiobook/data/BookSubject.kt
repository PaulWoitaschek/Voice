package de.ph1b.audiobook.data

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout


class BookSubject private constructor(failureMetaData: FailureMetadata, actual: Book?) :
  Subject<BookSubject, Book?>(failureMetaData, actual) {

  fun positionIs(position: Int) {
    if (actual()?.position != position) {
      fail("positionIs", position)
    }
  }

  fun durationIs(duration: Int) {
    if (actual()?.duration != duration) {
      fail("durationIs", duration)
    }
  }

  fun currentChapterIs(currentChapter: Chapter) {
    if (actual()?.currentChapter != currentChapter) {
      fail("currentChapterIs", currentChapter)
    }
  }

  fun currentChapterIndexIs(currentChapterIndex: Int) {
    if (actual()?.currentChapterIndex != currentChapterIndex) {
      fail("currentChapterIndexIs", currentChapterIndex)
    }
  }

  fun nextChapterIs(nextChapter: Chapter?) {
    if (actual()?.nextChapter != nextChapter) {
      fail("nextChapterIs", arrayOf(nextChapter))
    }
  }

  fun previousChapterIs(previousChapter: Chapter?) {
    if (actual()?.previousChapter != previousChapter) {
      fail("previousChapterIs", arrayOf(previousChapter))
    }
  }

  companion object {
    val factory = Factory(::BookSubject)
  }
}

fun Book?.assertThat(): BookSubject = assertAbout(BookSubject.factory).that(this)
