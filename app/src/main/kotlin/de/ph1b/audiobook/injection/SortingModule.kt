package de.ph1b.audiobook.injection

import com.squareup.anvil.annotations.ContributesTo
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.paulwoitaschek.flowpref.Pref
import de.paulwoitaschek.flowpref.android.AndroidPreferences
import de.paulwoitaschek.flowpref.android.enum
import de.ph1b.audiobook.AppScope
import de.ph1b.audiobook.data.BookComparator
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import javax.inject.Singleton

@MapKey
annotation class BookOverviewCategoryKey(val value: BookOverviewCategory)

@Module
@ContributesTo(AppScope::class)
object SortingModule {

  @Provides
  @Singleton
  @IntoMap
  @BookOverviewCategoryKey(BookOverviewCategory.CURRENT)
  fun currentComparatorPref(prefs: AndroidPreferences): Pref<BookComparator> {
    return prefs.enum(BookOverviewCategory.CURRENT.name, BookComparator.ByName)
  }

  @Provides
  @Singleton
  @IntoMap
  @BookOverviewCategoryKey(BookOverviewCategory.NOT_STARTED)
  fun notStartedComparatorPref(prefs: AndroidPreferences): Pref<BookComparator> {
    return prefs.enum(BookOverviewCategory.NOT_STARTED.name, BookComparator.ByName, BookComparator::class.java)
  }

  @Provides
  @Singleton
  @IntoMap
  @BookOverviewCategoryKey(BookOverviewCategory.FINISHED)
  fun finishedComparatorPref(prefs: AndroidPreferences): Pref<BookComparator> {
    return prefs.enum(BookOverviewCategory.FINISHED.name, BookComparator.ByName, BookComparator::class.java)
  }
}
