package voice.playback.androidauto

import androidx.datastore.core.DataStore
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import voice.common.BookId
import voice.common.pref.CurrentBook
import voice.data.repo.BookRepository
import voice.playback.di.PlaybackScope
import voice.playback.session.ChangeNotifier
import javax.inject.Inject

/**
 * Notifies about changes upon android auto connection.
 */
@PlaybackScope
class NotifyOnAutoConnectionChange
@Inject constructor(
  private val changeNotifier: ChangeNotifier,
  private val repo: BookRepository,
  @CurrentBook
  private val currentBook: DataStore<BookId?>,
  private val autoConnection: AndroidAutoConnectedReceiver,
) {

  suspend fun listen() {
    autoConnection.stream
      .filter { it }
      .mapNotNull {
        currentBook.data.first()?.let { repo.get(it) }
      }
      .collect { book ->
        // display the current book but don't play it
        changeNotifier.updateMetadata(book)
      }
  }
}
