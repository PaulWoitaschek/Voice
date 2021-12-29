package de.ph1b.audiobook.features.widget

import de.paulwoitaschek.flowpref.Pref
import de.ph1b.audiobook.common.pref.PrefKeys
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
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named
import javax.inject.Singleton

@Singleton
class TriggerWidgetOnChange
@Inject constructor(
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
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

  private fun anythingChanged(): Flow<Any> {
    return merge(currentBookChanged(), playStateChanged(), bookIdChanged())
  }

  private fun bookIdChanged(): Flow<UUID> {
    return currentBookIdPref.flow.distinctUntilChanged()
  }

  private fun playStateChanged(): Flow<PlayStateManager.PlayState> {
    return playStateManager.playStateFlow().distinctUntilChanged()
  }

  private fun currentBookChanged(): Flow<Book> {
    return currentBookIdPref.flow
      .flatMapLatest { id ->
        repo.flow(id)
      }
      .filterNotNull()
      .distinctUntilChanged { previous, current ->
        previous.id == current.id &&
          previous.content.chapters == current.content.chapters &&
          previous.content.currentFile == current.content.currentFile
      }
  }
}
