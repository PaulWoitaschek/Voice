package voice.bookOverview.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesGraphExtension
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.Scope
import voice.bookOverview.bottomSheet.BottomSheetViewModel
import voice.bookOverview.deleteBook.DeleteBookViewModel
import voice.bookOverview.editTitle.EditBookTitleViewModel
import voice.bookOverview.fileCover.FileCoverViewModel
import voice.bookOverview.overview.BookOverviewViewModel

@Scope
annotation class BookOverviewScope

@ContributesGraphExtension(scope = BookOverviewScope::class)
@BookOverviewScope
interface BookOverviewGraph {
  val bookOverviewViewModel: BookOverviewViewModel
  val editBookTitleViewModel: EditBookTitleViewModel
  val bottomSheetViewModel: BottomSheetViewModel
  val deleteBookViewModel: DeleteBookViewModel
  val fileCoverViewModel: FileCoverViewModel

  @ContributesGraphExtension.Factory(AppScope::class)
  interface Factory {
    fun create(): BookOverviewGraph

    @ContributesTo(AppScope::class)
    interface Provider {
      val bookOverviewGraphProviderFactory: Factory
    }
  }
}
