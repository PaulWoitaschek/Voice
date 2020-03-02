package de.ph1b.audiobook.playback.androidauto

import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.di.PerService
import de.ph1b.audiobook.playback.session.ChangeNotifier
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.filter
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

/**
 * Notifies about changes upon android auto connection.
 */
@PerService
class NotifyOnAutoConnectionChange
@Inject constructor(
  private val changeNotifier: ChangeNotifier,
  private val repo: BookRepository,
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
  private val autoConnection: AndroidAutoConnectedReceiver
) {

  suspend fun listen() {
    autoConnection.stream
      .filter { it }
      .collect {
        // display the current book but don't play it
        val book = repo.bookById(currentBookIdPref.value)
        if (book != null) {
          changeNotifier.updateMetadata(book)
        }
      }
  }
}
