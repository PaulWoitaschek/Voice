package voice.features.widget

import android.app.Application
import androidx.datastore.core.DataStore
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesIntoSet
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onEach
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.initializer.AppInitializer
import voice.core.playback.playstate.PlayStateManager

@ContributesIntoSet(AppScope::class)
@Inject
class TriggerWidgetOnChange(
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val repo: BookRepository,
  private val playStateManager: PlayStateManager,
  private val widgetUpdater: WidgetUpdater,
  private val scope: CoroutineScope,
) : AppInitializer {

  override fun onAppStart(application: Application) {
    anythingChanged()
      .onEach {
        widgetUpdater.update()
      }
      .launchIn(scope)
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
