package de.ph1b.audiobook.features.bookOverview.list

import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.book
import de.ph1b.audiobook.data.BookComparator
import org.junit.Test

class BookComparatorTest {

  private val b1 = book(name = "A", lastPlayedAtMillis = 0, addedAtMillis = 2)
  private val b2 = book(name = "B", lastPlayedAtMillis = 0, addedAtMillis = 2)
  private val b3 = book(name = "B", lastPlayedAtMillis = 2, addedAtMillis = 1)
  private val b4 = book(name = "D", lastPlayedAtMillis = 5, addedAtMillis = 7)
  private val b5 = book(name = "C", lastPlayedAtMillis = 5, addedAtMillis = 6)
  private val books = listOf(b1, b2, b3, b4, b5)

  @Test
  fun byLastPlayed_thenName() {
    val sorted = books.sortedWith(BookComparator.ByLastPlayed)
    assertThat(sorted).containsExactly(b5, b4, b3, b1, b2).inOrder()
  }

  @Test
  fun byName_thenLastTime() {
    val sorted = books.sortedWith(BookComparator.ByName)
    assertThat(sorted).containsExactly(b1, b3, b2, b5, b4).inOrder()
  }

  @Test
  fun byDateAdded() {
    val sorted = books.sortedWith(BookComparator.ByDateAdded)
    assertThat(sorted).containsExactly(b4, b5, b1, b2, b3).inOrder()
  }
}
