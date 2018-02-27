package de.ph1b.audiobook.features.widget

import dagger.Reusable
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import io.reactivex.Observable
import javax.inject.Inject
import javax.inject.Named

@Reusable
class TriggerWidgetOnChange @Inject constructor(
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<Long>,
  private val repo: BookRepository,
  private val playStateManager: PlayStateManager,
  private val widgetUpdater: WidgetUpdater
) {

  fun init() {
    val anythingChanged: Observable<Any> = anythingChanged()
    anythingChanged.subscribe { widgetUpdater.update() }
  }

  private fun anythingChanged(): Observable<Any> =
    Observable.merge(currentBookChanged(), playStateChanged(), bookIdChanged())

  private fun bookIdChanged(): Observable<Long> = currentBookIdPref.stream
    .distinctUntilChanged()

  private fun playStateChanged(): Observable<PlayStateManager.PlayState> =
    playStateManager.playStateStream()
      .distinctUntilChanged()

  private fun currentBookChanged(): Observable<Book> = repo.updateObservable()
    .filter { it.id == currentBookIdPref.value }
    .distinctUntilChanged { previous, current ->
      previous.id == current.id
          && previous.chapters == current.chapters
          && previous.currentFile == current.currentFile
    }
}
