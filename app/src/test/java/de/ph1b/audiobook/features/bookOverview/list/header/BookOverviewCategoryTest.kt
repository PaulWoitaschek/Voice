package de.ph1b.audiobook.features.bookOverview.list.header

import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.data.Book
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
    println(book)
    println(book.content.position)
    println(book.content.duration)
    assertThat(book.categories()).containsExactly(BookOverviewCategory.FINISHED)
  }

  @Test
  fun notStarted() {
    val book = BookFactory.create().updateContent {
      updateSettings {
        val firstChapter = chapters.first()
        copy(currentFile = firstChapter.file, positionInChapter = 0)
      }
    }
    assertThat(book.categories()).containsExactly(BookOverviewCategory.NOT_STARTED)
  }

  @Test
  fun current() {
    val book = BookFactory.create().updateContent {
      updateSettings {
        val lastChapter = chapters.last()
        copy(currentFile = lastChapter.file, positionInChapter = 0)
      }
    }
    println(book)
    assertThat(book.categories()).containsExactly(BookOverviewCategory.CURRENT)
  }

  private fun Book.categories(): List<BookOverviewCategory> {
    return BookOverviewCategory.values().filter { categoryToCheck ->
      categoryToCheck.filter(this)
    }
  }
}
