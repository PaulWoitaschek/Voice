package de.ph1b.audiobook.injection

import com.f2prateek.rx.preferences2.RxSharedPreferences
import dagger.MapKey
import dagger.Module
import dagger.Provides
import dagger.multibindings.IntoMap
import de.ph1b.audiobook.data.BookComparator
import de.ph1b.audiobook.features.bookOverview.list.header.BookOverviewCategory
import de.ph1b.audiobook.persistence.pref.PersistentPref
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
  fun currentComparatorPref(prefs: RxSharedPreferences): Pref<BookComparator> {
    val pref = prefs.getEnum(BookOverviewCategory.CURRENT.name, BookComparator.BY_NAME, BookComparator::class.java)
    return PersistentPref(pref)
  }

  @JvmStatic
  @Provides
  @Singleton
  @IntoMap
  @BookOverviewCategoryKey(BookOverviewCategory.NOT_STARTED)
  fun notStartedComparatorPref(prefs: RxSharedPreferences): Pref<BookComparator> {
    val pref = prefs.getEnum(BookOverviewCategory.NOT_STARTED.name, BookComparator.BY_NAME, BookComparator::class.java)
    return PersistentPref(pref)
  }

  @JvmStatic
  @Provides
  @Singleton
  @IntoMap
  @BookOverviewCategoryKey(BookOverviewCategory.FINISHED)
  fun finishedComparatorPref(prefs: RxSharedPreferences): Pref<BookComparator> {
    val pref = prefs.getEnum(BookOverviewCategory.FINISHED.name, BookComparator.BY_NAME, BookComparator::class.java)
    return PersistentPref(pref)
  }
}
