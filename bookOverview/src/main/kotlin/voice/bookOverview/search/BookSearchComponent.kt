package voice.bookOverview.search

import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope

@ContributesTo(AppScope::class)
interface BookSearchComponent {
  val bookSearchViewModel: BookSearchViewModel
}
