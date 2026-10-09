package voice.features.bookOverview.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import voice.core.data.BookId
import voice.features.bookOverview.bottomSheet.BottomSheetViewModel
import voice.features.bookOverview.deleteBook.DeleteBookViewModel
import voice.features.bookOverview.editTitle.EditBookTitleViewModel
import voice.features.bookOverview.fileCover.FileCoverViewModel

abstract class BookActionsScope private constructor()

@GraphExtension(scope = BookActionsScope::class)
interface BookActionsGraph {
  val bottomSheetViewModel: BottomSheetViewModel
  val editBookTitleViewModel: EditBookTitleViewModel
  val deleteBookViewModel: DeleteBookViewModel
  val fileCoverViewModel: FileCoverViewModel

  @GraphExtension.Factory
  @ContributesTo(AppScope::class)
  interface Factory {
    fun createBookActionsGraph(@Provides bookId: BookId): BookActionsGraph

    @ContributesTo(AppScope::class)
    interface Provider {
      val bookActionsGraphProviderFactory: Factory
    }
  }
}
