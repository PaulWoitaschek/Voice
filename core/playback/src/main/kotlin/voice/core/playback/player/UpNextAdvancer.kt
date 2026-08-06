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

  private suspend fun advance() {
    val upNextId = upNextBookStore.data.first() ?: return
    val book = bookRepository.get(upNextId) ?: run {
      Logger.w("Up-next book $upNextId not found, clearing store")
      upNextBookStore.updateData { null }
      return
    }

    Logger.d("Advancing to up-next book: ${book.content.name}")

    // positionInChapter = 1L (not 0L) so Book.category returns CURRENT, not NOT_STARTED
    bookRepository.updateBook(upNextId) { content ->
      content.copy(
        currentChapter = book.chapters.first().id,
        positionInChapter = 1L,
      )
    }

    currentBookStore.updateData { upNextId }
    upNextBookStore.updateData { null }

    player.setMediaItem(mediaItemProvider.mediaItem(book))
    player.prepare()
    player.play()
  }
}
