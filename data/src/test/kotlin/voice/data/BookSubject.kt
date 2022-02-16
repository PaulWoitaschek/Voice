package voice.data

import com.google.common.truth.FailureMetadata
import com.google.common.truth.Subject
import com.google.common.truth.Subject.Factory
import com.google.common.truth.Truth.assertAbout

class BookSubject private constructor(failureMetaData: FailureMetadata, private val actual: Book?) :
  Subject(failureMetaData, actual) {

  fun positionIs(position: Long) {
    if (actual?.position != position) {
      failWithActual("position", position)
    }
  }

  fun durationIs(duration: Long) {
    if (actual?.duration != duration) {
      failWithActual("duration", duration)
    }
  }

  fun currentChapterIs(currentChapter: Chapter) {
    if (actual?.currentChapter != currentChapter) {
      failWithActual("currentChapter", currentChapter)
    }
  }

  fun currentChapterIndexIs(currentChapterIndex: Int) {
    if (actual?.content?.currentChapterIndex != currentChapterIndex) {
      failWithActual("currentChapterIndex", currentChapterIndex)
    }
  }

  fun nextChapterIs(nextChapter: Chapter?) {
    if (actual?.nextChapter != nextChapter) {
      failWithActual("nextChapter", arrayOf(nextChapter))
    }
  }

  fun previousChapterIs(previousChapter: Chapter?) {
    if (actual?.previousChapter != previousChapter) {
      failWithActual("previousChapter", arrayOf(previousChapter))
    }
  }

  companion object {
    val factory = Factory(::BookSubject)
  }
}

fun Book?.assertThat(): BookSubject = assertAbout(BookSubject.factory).that(this)
