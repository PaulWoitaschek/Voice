package de.ph1b.audiobook.features.bookOverview.list.header

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.BookFactory
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookOverviewCategoryTest {

  @Test
  fun finished() {
    val book = BookFactory.create().let { book ->
      val lastChapter = book.chapters.last()
      book.copy(
        content = book.content.copy(
          currentChapter = lastChapter.uri,
          positionInChapter = lastChapter.duration
        )
      )
    }
    assertThat(book.category).isEqualTo(BookOverviewCategory.FINISHED)
  }

  @Test
  fun notStarted() {
    val book = BookFactory.create().let { book ->
      val firstChapter = book.chapters.first()
      book.copy(
        content = book.content.copy(
          currentChapter = firstChapter.uri,
          positionInChapter = 0
        )
      )
    }
    assertThat(book.category).isEqualTo(BookOverviewCategory.NOT_STARTED)
  }

  @Test
  fun current() {
    val book = BookFactory.create().let { book ->
      book.copy(
        content = book.content.copy(
          currentChapter = book.chapters.last().uri,
          positionInChapter = 0
        )
      )
    }
    println(book.position)
    println(book.duration)
    assertThat(book.category).isEqualTo(BookOverviewCategory.CURRENT)
  }
}
