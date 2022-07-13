package voice.bookOverview.di

import com.squareup.anvil.annotations.ContributesTo
import dagger.BindsInstance
import dagger.Subcomponent
import voice.bookOverview.bottomSheet.BottomSheetViewModel
import voice.bookOverview.editTitle.EditBookTitleViewModel
import voice.bookOverview.overview.BookOverviewNavigator
import voice.bookOverview.overview.BookOverviewViewModel
import voice.common.AppScope
import javax.inject.Scope

@Scope
annotation class BookOverviewScope

@Subcomponent
@BookOverviewScope
interface BookOverviewComponent {
  val bookOverviewViewModel: BookOverviewViewModel
  val editBookTitleViewModel: EditBookTitleViewModel
  val bottomSheetViewModel: BottomSheetViewModel

  @Subcomponent.Factory
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
