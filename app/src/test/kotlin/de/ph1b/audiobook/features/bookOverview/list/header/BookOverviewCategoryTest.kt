package de.ph1b.audiobook.features.bookOverview.list.header

import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.book
import org.junit.Test

class BookOverviewCategoryTest {

  @Test
  fun finished() {
    val book = book().let { book ->
      val lastChapter = book.chapters.last()
      book.copy(
        content = book.content.copy(
          currentChapter = lastChapter.id,
          positionInChapter = lastChapter.duration
        )
      )
    }
    assertThat(book.category).isEqualTo(BookOverviewCategory.FINISHED)
  }

  @Test
  fun notStarted() {
    val book = book().let { book ->
      val firstChapter = book.chapters.first()
      book.copy(
        content = book.content.copy(
          currentChapter = firstChapter.id,
          positionInChapter = 0
        )
      )
    }
    assertThat(book.category).isEqualTo(BookOverviewCategory.NOT_STARTED)
  }

  @Test
  fun current() {
    val book = book().let { book ->
      book.copy(
        content = book.content.copy(
          currentChapter = book.chapters.last().id,
          positionInChapter = 0
        )
      )
    }
    assertThat(book.category).isEqualTo(BookOverviewCategory.CURRENT)
  }
}
