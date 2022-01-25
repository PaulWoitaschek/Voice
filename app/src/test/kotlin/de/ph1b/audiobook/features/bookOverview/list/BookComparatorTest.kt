package de.ph1b.audiobook.features.bookOverview.list

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import de.ph1b.audiobook.BookFactory
import de.ph1b.audiobook.data.BookComparator
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class BookComparatorTest {

  private val b1 = BookFactory.create(name = "A", lastPlayedAtMillis = 0, addedAtMillis = 2)
  private val b2 = BookFactory.create(name = "B", lastPlayedAtMillis = 0, addedAtMillis = 2)
  private val b3 = BookFactory.create(name = "B", lastPlayedAtMillis = 2, addedAtMillis = 1)
  private val b4 = BookFactory.create(name = "D", lastPlayedAtMillis = 5, addedAtMillis = 7)
  private val b5 = BookFactory.create(name = "C", lastPlayedAtMillis = 5, addedAtMillis = 6)
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
