package de.ph1b.audiobook.features.widget

import android.annotation.SuppressLint
import de.ph1b.audiobook.common.getIfPresent
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import io.reactivex.Observable
import java.util.UUID
import javax.inject.Named

class TriggerWidgetOnChange(
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
  private val repo: BookRepository,
  private val playStateManager: PlayStateManager,
  private val widgetUpdater: WidgetUpdater
) {

  @SuppressLint("CheckResult")
  fun init() {
    val anythingChanged: Observable<Any> = anythingChanged()
    anythingChanged.subscribe { widgetUpdater.update() }
  }

  private fun anythingChanged(): Observable<Any> =
    Observable.merge(currentBookChanged(), playStateChanged(), bookIdChanged())

  private fun bookIdChanged(): Observable<UUID> = currentBookIdPref.stream
    .distinctUntilChanged()

  private fun playStateChanged(): Observable<PlayStateManager.PlayState> =
    playStateManager.playStateStream()
      .distinctUntilChanged()

  private fun currentBookChanged(): Observable<Book> {
    return currentBookIdPref.stream
      .switchMap {
        repo.byId(it).getIfPresent()
      }
      .distinctUntilChanged { previous, current ->
        previous.id == current.id
            && previous.content.chapters == current.content.chapters
            && previous.content.currentFile == current.content.currentFile
      }
  }
}
