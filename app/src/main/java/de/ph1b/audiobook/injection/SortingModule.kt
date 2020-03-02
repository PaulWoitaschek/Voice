package de.ph1b.audiobook.injection

import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.ph1b.audiobook.data.BookComparator
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.persistence.pref.ReactivePrefs
import de.ph1b.audiobook.prefs.Pref
import javax.inject.Singleton

@MapKey
annotation class BookOverviewCategoryKey(val value: BookOverviewCategory)

@Module
object SortingModule {

  @JvmStatic
  @Provides
  @Singleton
  @IntoMap
  @BookOverviewCategoryKey(BookOverviewCategory.CURRENT)
  fun currentComparatorPref(prefs: ReactivePrefs): Pref<BookComparator> {
    return prefs.enum(BookOverviewCategory.CURRENT.name, BookComparator.BY_NAME)
  }

  @JvmStatic
  @Provides
  @Singleton
  @IntoMap
  @BookOverviewCategoryKey(BookOverviewCategory.NOT_STARTED)
  fun notStartedComparatorPref(prefs: ReactivePrefs): Pref<BookComparator> {
    return prefs.enum(BookOverviewCategory.NOT_STARTED.name, BookComparator.BY_NAME, BookComparator::class.java)
  }

  @JvmStatic
  @Provides
  @Singleton
  @IntoMap
  @BookOverviewCategoryKey(BookOverviewCategory.FINISHED)
  fun finishedComparatorPref(prefs: ReactivePrefs): Pref<BookComparator> {
    return prefs.enum(BookOverviewCategory.FINISHED.name, BookComparator.BY_NAME, BookComparator::class.java)
  }
}
