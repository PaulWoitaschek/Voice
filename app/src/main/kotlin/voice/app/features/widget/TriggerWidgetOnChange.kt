package voice.app.features.widget

import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.playstate.PlayStateManager

@SingleIn(AppScope::class)
@Inject
class TriggerWidgetOnChange(
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val repo: BookRepository,
  private val playStateManager: PlayStateManager,
  private val widgetUpdater: WidgetUpdater,
) {

  fun init() {
    MainScope().launch {
      anythingChanged().collect {
        widgetUpdater.update()
      }
    }
  }

  private fun anythingChanged(): Flow<Any?> {
    return merge(currentBookChanged(), playStateChanged(), bookIdChanged())
  }

  private fun bookIdChanged(): Flow<BookId?> {
    return currentBookStore.data.distinctUntilChanged()
  }

  private fun playStateChanged(): Flow<PlayStateManager.PlayState> {
    return playStateManager.flow
  }

  private fun currentBookChanged(): Flow<Book> {
    return currentBookStore.data.filterNotNull()
      .flatMapLatest { id ->
        repo.flow(id)
      }
      .filterNotNull()
      .distinctUntilChanged { previous, current ->
        previous.id == current.id &&
          previous.content.chapters == current.content.chapters &&
          previous.content.currentChapter == current.content.currentChapter
      }
  }
}
