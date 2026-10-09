package voice.features.bookOverview.di

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.GraphExtension
import voice.features.bookOverview.overview.BookOverviewViewModel
import voice.features.bookOverview.search.BookSearchViewModel

abstract class BookOverviewScope private constructor()

@GraphExtension(scope = BookOverviewScope::class)
interface BookOverviewGraph {
  val bookOverviewViewModel: BookOverviewViewModel
  val bookSearchViewModel: BookSearchViewModel

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
