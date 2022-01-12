package de.ph1b.audiobook.features.bookCategory

import android.net.Uri
import androidx.datastore.core.DataStore
import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.common.pref.PrefKeys
import de.ph1b.audiobook.data.BookComparator
import de.ph1b.audiobook.data.repo.BookRepo2
import de.ph1b.audiobook.features.bookOverview.GridMode
import de.ph1b.audiobook.features.bookOverview.list.BookOverviewViewState
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.features.gridCount.GridCount
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Named

class BookCategoryViewModel
@Inject constructor(
  private val repo: BookRepo2,
  @CurrentBook
  private val currentBookId: DataStore<Uri?>,
  @Named(PrefKeys.GRID_MODE)
  private val gridModePref: Pref<GridMode>,
  private val gridCount: GridCount,
  private val comparatorPrefForCategory: @JvmSuppressWildcards Map<BookOverviewCategory, Pref<BookComparator>>
) {

  lateinit var category: BookOverviewCategory

  private fun comparatorPref(): Pref<BookComparator> = comparatorPrefForCategory.getValue(category)

  fun get(): Flow<BookCategoryState> {
    val comparatorStream = comparatorPref().flow
    return combine(
      gridModePref.flow,
      repo.flow(),
      comparatorStream,
      currentBookId.data,
    ) { gridMode, books, comparator, currentBookId ->
      val gridColumnCount = gridCount.gridColumnCount(gridMode)
      val models = books.asSequence()
        .filter(category.filter)
        .sortedWith(comparator)
        .map { book ->
          BookOverviewViewState(book, gridColumnCount, currentBookId)
        }
        .toList()
      BookCategoryState(gridColumnCount, models)
    }
  }

  fun sort(comparator: BookComparator) {
    comparatorPref().value = comparator
  }

  fun bookSorting(): BookComparator {
    return comparatorPref().value
  }
}

data class BookCategoryState(
  val gridColumnCount: Int,
  val models: List<BookOverviewViewState>
)
