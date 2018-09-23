package de.ph1b.audiobook.playback

import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.injection.PerService
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.utils.ChangeNotifier
import io.reactivex.disposables.Disposable
import kotlinx.coroutines.experimental.GlobalScope
import kotlinx.coroutines.experimental.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

/**
 * Notifies about changes upon android auto connection.
 */
@PerService
class NotifyOnAutoConnectionChange @Inject constructor(
  private val changeNotifier: ChangeNotifier,
  private val repo: BookRepository,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
  private val autoConnection: AndroidAutoConnectedReceiver
) {

  private var listeningDisposable: Disposable? = null

  fun listen() {
    if (listeningDisposable?.isDisposed != false) {
      listeningDisposable = autoConnection.stream
        .filter { it }
        .subscribe {
          // display the current book but don't play it
          GlobalScope.launch {
            repo.bookById(currentBookIdPref.value)?.let { book ->
              changeNotifier.notify(ChangeNotifier.Type.METADATA, book, true)
            }
          }
        }
    }
  }

  fun unregister() {
    listeningDisposable?.dispose()
  }
}
