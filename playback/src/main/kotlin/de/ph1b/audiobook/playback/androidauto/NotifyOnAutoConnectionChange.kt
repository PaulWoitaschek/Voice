package de.ph1b.audiobook.playback.androidauto

import androidx.datastore.core.DataStore
import de.ph1b.audiobook.common.pref.CurrentBook
import de.ph1b.audiobook.data.Book
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.playback.di.PlaybackScope
import de.ph1b.audiobook.playback.session.ChangeNotifier
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
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
  private val currentBook: DataStore<Book.Id?>,
  private val autoConnection: AndroidAutoConnectedReceiver
) {

  suspend fun listen() {
    autoConnection.stream
      .filter { it }
      .mapNotNull {
        currentBook.data.first()?.let { repo.flow(it).first() }
      }
      .collect { book ->
        // display the current book but don't play it
        changeNotifier.updateMetadata(book)
      }
  }
}
