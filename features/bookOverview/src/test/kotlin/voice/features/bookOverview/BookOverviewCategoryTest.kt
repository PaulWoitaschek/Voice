package voice.features.bookOverview

import voice.features.bookOverview.overview.BookOverviewCategory
import voice.features.bookOverview.overview.category
import kotlin.test.Test
import kotlin.test.assertEquals

class BookOverviewCategoryTest {

  @Test
  fun finished() {
    val book = book().let { book ->
      val lastChapter = book.chapters.last()
      book.copy(
        content = book.content.copy(
          currentChapter = lastChapter.id,
          positionInChapter = lastChapter.duration,
        ),
      )
    }
    assertEquals(expected = BookOverviewCategory.FINISHED, actual = book.category)
  }

  @Test
  fun notStarted() {
    val book = book().let { book ->
      val firstChapter = book.chapters.first()
      book.copy(
        content = book.content.copy(
          currentChapter = firstChapter.id,
          positionInChapter = 0,
        ),
      )
    }
    assertEquals(expected = BookOverviewCategory.NOT_STARTED, actual = book.category)
  }

  @Test
  fun current() {
    val book = book().let { book ->
      book.copy(
        content = book.content.copy(
          currentChapter = book.chapters.last().id,
          positionInChapter = 0,
        ),
      )
    }
    assertEquals(expected = BookOverviewCategory.CURRENT, actual = book.category)
  }
}
