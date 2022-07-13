package voice.bookOverview.di

import com.squareup.anvil.annotations.ContributesSubcomponent
import com.squareup.anvil.annotations.ContributesTo
import dagger.BindsInstance
import voice.bookOverview.bottomSheet.BottomSheetViewModel
import voice.bookOverview.editTitle.EditBookTitleViewModel
import voice.bookOverview.overview.BookOverviewNavigator
import voice.bookOverview.overview.BookOverviewViewModel
import voice.common.AppScope
import javax.inject.Scope

@Scope
annotation class BookOverviewScope

@ContributesSubcomponent(scope = BookOverviewScope::class, parentScope = AppScope::class)
@BookOverviewScope
interface BookOverviewComponent {
  val bookOverviewViewModel: BookOverviewViewModel
  val editBookTitleViewModel: EditBookTitleViewModel
  val bottomSheetViewModel: BottomSheetViewModel

  @ContributesSubcomponent.Factory
  interface Factory {
    fun create(
      @BindsInstance
      navigator: BookOverviewNavigator,
    ): BookOverviewComponent

    @ContributesTo(AppScope::class)
    interface Provider {
      val bookOverviewComponentProviderFactory: Factory
    }
  }
}
