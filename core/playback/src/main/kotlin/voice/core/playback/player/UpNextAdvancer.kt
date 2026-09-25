package voice.core.playback.player

import androidx.datastore.core.DataStore
import androidx.media3.common.Player
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.data.store.UpNextBookStore
import voice.core.logging.api.Logger
import voice.core.playback.session.MediaItemProvider

@Inject
class UpNextAdvancer(
  @UpNextBookStore
  private val upNextBookStore: DataStore<BookId?>,
  @CurrentBookStore
  private val currentBookStore: DataStore<BookId?>,
  private val bookRepository: BookRepository,
  private val mediaItemProvider: MediaItemProvider,
  private val scope: CoroutineScope,
) : Player.Listener {

  private lateinit var player: Player

  fun attachTo(player: Player) {
    this.player = player
    player.addListener(this)
  }

  override fun onPlaybackStateChanged(playbackState: Int) {
    if (playbackState != Player.STATE_ENDED) return
    scope.launch { advance() }
  }

  override fun onIsPlayingChanged(isPlaying: Boolean) {
    if (!isPlaying) return
    scope.launch { clearUpNextIfNowPlaying() }
  }

  // Loading a book into the session (e.g. to adjust its volume boost or speed from its detail
  // screen) doesn't mean it's being played, so we only drop its up-next status once it's
  // actually playing — regardless of what triggered playback (library tap, widget, Bluetooth).
  private suspend fun clearUpNextIfNowPlaying() {
    val upNextId = upNextBookStore.data.first() ?: return
    if (currentBookStore.data.first() == upNextId) {
      upNextBookStore.updateData { null }
    }
  }

  private suspend fun advance() {
    val upNextId = upNextBookStore.data.first() ?: return
    val book = bookRepository.get(upNextId)
    if (book == null || !book.content.isActive) {
      Logger.w("Up-next book $upNextId not found or deleted, clearing store")
      upNextBookStore.updateData { null }
      return
    }

    Logger.d("Advancing to up-next book: ${book.content.name}")

    // Commit the pointer swap before any further suspension so a concurrent read of
    // currentBookStore (e.g. Player.onPlaybackResumption) can't observe the book that just
    // ended instead of the one we're advancing to.
    currentBookStore.updateData { upNextId }
    upNextBookStore.updateData { null }

    // positionInChapter = 1L (not 0L) so Book.category returns CURRENT, not NOT_STARTED
    bookRepository.updateBook(upNextId) { content ->
      content.copy(
        currentChapter = book.chapters.first().id,
        positionInChapter = 1L,
      )
    }

    player.setMediaItem(mediaItemProvider.mediaItem(book))
    player.prepare()
    player.play()
  }
}
