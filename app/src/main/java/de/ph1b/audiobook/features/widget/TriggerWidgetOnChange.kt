package de.ph1b.audiobook.features.widget

import dagger.Reusable
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.misc.asV2Observable
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import io.reactivex.Observable
import javax.inject.Inject


@Reusable
class TriggerWidgetOnChange @Inject constructor(
    private val prefs: PrefsManager,
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

  private fun bookIdChanged(): Observable<Long> = prefs.currentBookId.asV2Observable()
      .distinctUntilChanged()

  private fun playStateChanged(): Observable<PlayStateManager.PlayState> = playStateManager.playStateStream()
      .distinctUntilChanged()

  private fun currentBookChanged(): Observable<Book> = repo.updateObservable()
      .filter { it.id == prefs.currentBookId.value }
      .distinctUntilChanged { previous, current ->
        previous.id == current.id
            && previous.chapters == current.chapters
            && previous.currentFile == current.currentFile
      }
}
