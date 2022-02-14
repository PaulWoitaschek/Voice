package de.ph1b.audiobook.features.widget

import androidx.datastore.core.DataStore
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriggerWidgetOnChange
@Inject constructor(
  @CurrentBook
  private val currentBook: DataStore<Book.Id?>,
  private val repo: BookRepository,
  private val playStateManager: PlayStateManager,
  private val widgetUpdater: WidgetUpdater
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

  private fun bookIdChanged(): Flow<Book.Id?> {
    return currentBook.data.distinctUntilChanged()
  }

  private fun playStateChanged(): Flow<PlayStateManager.PlayState> {
    return playStateManager.playStateFlow().distinctUntilChanged()
  }

  private fun currentBookChanged(): Flow<Book> {
    return currentBook.data.filterNotNull()
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
