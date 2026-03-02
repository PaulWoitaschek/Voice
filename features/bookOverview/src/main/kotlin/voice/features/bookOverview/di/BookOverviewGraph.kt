package voice.features.bookOverview.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import voice.features.bookOverview.bottomSheet.BottomSheetViewModel
import voice.features.bookOverview.deleteBook.DeleteBookViewModel
import voice.features.bookOverview.editTitle.EditBookTitleViewModel
import voice.features.bookOverview.fileCover.FileCoverViewModel
import voice.features.bookOverview.overview.BookOverviewViewModel

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
