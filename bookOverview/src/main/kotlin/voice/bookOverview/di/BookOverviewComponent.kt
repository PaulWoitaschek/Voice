package voice.bookOverview.di

import dev.zacsweers.metro.ContributesGraphExtension
import dev.zacsweers.metro.ContributesTo
import voice.bookOverview.bottomSheet.BottomSheetViewModel
import voice.bookOverview.deleteBook.DeleteBookViewModel
import voice.bookOverview.editTitle.EditBookTitleViewModel
import voice.bookOverview.fileCover.FileCoverViewModel
import voice.bookOverview.overview.BookOverviewViewModel
import voice.common.AppScope
import javax.inject.Scope

@Scope
annotation class BookOverviewScope

@ContributesGraphExtension(scope = BookOverviewScope::class)
@BookOverviewScope
interface BookOverviewComponent {
  val bookOverviewViewModel: BookOverviewViewModel
  val editBookTitleViewModel: EditBookTitleViewModel
  val bottomSheetViewModel: BottomSheetViewModel
  val deleteBookViewModel: DeleteBookViewModel
  val fileCoverViewModel: FileCoverViewModel

  @ContributesGraphExtension.Factory(AppScope::class)
  interface Factory {
    fun create(): BookOverviewComponent

    @ContributesTo(AppScope::class)
    interface Provider {
      val bookOverviewComponentProviderFactory: Factory
    }
  }
}
