package voice.features.bookOverview.select

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.map
import voice.core.data.BookId

@SingleIn(AppScope::class)
@Inject
class BookSelectionRequests {

  // no replay, so only a screen that's on screen under the actions starts selecting
  internal val requests: SharedFlow<List<BookId>>
    field = MutableSharedFlow<List<BookId>>(extraBufferCapacity = 1)

  // a screen that can select collects the requests while it's on screen, so the book actions of the
  // player don't offer it
  internal val canSelect: Flow<Boolean>
    get() = requests.subscriptionCount.map { it > 0 }

  internal fun select(ids: List<BookId>) {
    requests.tryEmit(ids)
  }
}
