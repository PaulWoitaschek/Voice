package voice.bookOverview.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Scope
import voice.bookOverview.bottomSheet.BottomSheetViewModel
import voice.bookOverview.deleteBook.DeleteBookViewModel
import voice.bookOverview.editTitle.EditBookTitleViewModel
import voice.bookOverview.fileCover.FileCoverViewModel
import voice.bookOverview.overview.BookOverviewViewModel

@Scope
annotation class BookOverviewScope

@GraphExtension(scope = BookOverviewScope::class)
@BookOverviewScope
interface BookOverviewGraph {
  val bookOverviewViewModel: BookOverviewViewModel
  val editBookTitleViewModel: EditBookTitleViewModel
  val bottomSheetViewModel: BottomSheetViewModel
  val deleteBookViewModel: DeleteBookViewModel
  val fileCoverViewModel: FileCoverViewModel

  @GraphExtension.Factory
  @ContributesTo(AppScope::class)
  interface Factory {
    fun create(): BookOverviewGraph

    @ContributesTo(AppScope::class)
    interface Provider {
      val bookOverviewGraphProviderFactory: Factory
    }
  }
}
