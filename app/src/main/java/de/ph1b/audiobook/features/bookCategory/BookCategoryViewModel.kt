package de.ph1b.audiobook.features.bookCategory

import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.features.bookOverview.GridMode
import de.ph1b.audiobook.features.bookOverview.list.BookComparator
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewModel
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.features.gridCount.GridCount
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.misc.Observables
import de.ph1b.audiobook.persistence.pref.Pref
import io.reactivex.Observable
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

class BookCategoryViewModel
@Inject constructor(
  private val repo: BookRepository,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
  @Named(PrefKeys.GRID_MODE)
  private val gridModePref: Pref<GridMode>,
  private val gridCount: GridCount
) {

  fun get(category: BookOverviewCategory): Observable<BookCategoryState> {
    return Observables.combineLatest(gridModePref.stream, repo.booksStream()) { gridMode, books ->
      val gridColumnCount = gridCount.gridColumnCount(gridMode)
      val currentBookId = currentBookIdPref.value
      val models = books.asSequence()
        .filter(category.filter)
        .sortedWith(BookComparator.BY_NAME)
        .map { book ->
          BookOverviewModel(
            book = book,
            isCurrentBook = book.id == currentBookId,
            useGridView = gridColumnCount > 1
          )
        }
        .toList()
      BookCategoryState(gridColumnCount, models)
    }
  }

  fun sort(comparator: BookComparator) {
  }

  fun bookSorting(): BookComparator {
    return BookComparator.BY_NAME
  }

}

data class BookCategoryState(
  val gridColumnCount: Int,
  val models: List<BookOverviewModel>
)
