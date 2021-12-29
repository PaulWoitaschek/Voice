package de.ph1b.audiobook.features.bookOverview.list

import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.data.BookComparator
import org.junit.Test

class BookComparatorTest {

  private val b1 = BookFactory.create(lastPlayedAtMillis = 0, name = "A", addedAtMillis = 2)
  private val b2 = BookFactory.create(lastPlayedAtMillis = 0, name = "B", addedAtMillis = 2)
  private val b3 = BookFactory.create(lastPlayedAtMillis = 2, name = "B", addedAtMillis = 1)
  private val b4 = BookFactory.create(lastPlayedAtMillis = 5, name = "D", addedAtMillis = 7)
  private val b5 = BookFactory.create(lastPlayedAtMillis = 5, name = "C", addedAtMillis = 6)
  private val books = listOf(b1, b2, b3, b4, b5)

  @Test
  fun byLastPlayed_thenName() {
    val sorted = books.sortedWith(BookComparator.BY_LAST_PLAYED)
    assertThat(sorted).containsExactly(b5, b4, b3, b1, b2).inOrder()
  }

  @Test
  fun byName_thenLastTime() {
    val sorted = books.sortedWith(BookComparator.BY_NAME)
    assertThat(sorted).containsExactly(b1, b3, b2, b5, b4).inOrder()
  }

  @Test
  fun byDateAdded() {
    val sorted = books.sortedWith(BookComparator.BY_DATE_ADDED)
    assertThat(sorted).containsExactly(b4, b5, b1, b2, b3).inOrder()
  }
}
