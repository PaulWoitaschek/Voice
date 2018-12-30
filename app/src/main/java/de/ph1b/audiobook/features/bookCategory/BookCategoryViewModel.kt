package de.ph1b.audiobook.features.bookCategory

import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewModel
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import io.reactivex.Observable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class BookCategoryViewModel
@Inject constructor(
  private val repo: BookRepository,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>
) {

  fun get(category: BookOverviewCategory): Observable<List<BookOverviewModel>> {
    return repo.booksStream()
      .map {
        val currentBookId = currentBookIdPref.value
        it.asSequence()
          .filter(category.filter)
          .sortedWith(category.comparator)
          .map { book -> BookOverviewModel(book, book.id == currentBookId) }
          .toList()
      }
  }
}
