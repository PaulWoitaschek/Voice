package de.ph1b.audiobook.features.bookOverview.list.header

import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.BookFactory
import org.junit.Test

class BookOverviewCategoryTest {

  @Test
  fun finished() {
    val book = BookFactory.create().updateContent {
      updateSettings {
        val lastChapter = chapters.last()
        copy(currentFile = lastChapter.file, positionInChapter = lastChapter.duration)
      }
    }
    assertThat(book.category).isEqualTo(BookOverviewCategory.FINISHED)
  }

  @Test
  fun notStarted() {
    val book = BookFactory.create().updateContent {
      updateSettings {
        val firstChapter = chapters.first()
        copy(currentFile = firstChapter.file, positionInChapter = 0)
      }
    }
    assertThat(book.category).isEqualTo(BookOverviewCategory.NOT_STARTED)
  }

  @Test
  fun current() {
    val book = BookFactory.create().updateContent {
      updateSettings {
        val lastChapter = chapters.last()
        copy(currentFile = lastChapter.file, positionInChapter = 0)
      }
    }
    assertThat(book.category).isEqualTo(BookOverviewCategory.CURRENT)
  }
}
