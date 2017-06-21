package de.ph1b.audiobook.features.bookmarks

import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.misc.value
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.BookmarkProvider
import de.ph1b.audiobook.persistence.PrefsManager
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import io.reactivex.disposables.CompositeDisposable
import javax.inject.Inject

/**
 * Presenter for the bookmark MVP
 *
 * @author Paul Woitaschek
 */
class BookmarkPresenter @Inject constructor(
    private val prefs: PrefsManager,
    private val repo: BookRepository,
    private val bookmarkProvider: BookmarkProvider,
    private val playStateManager: PlayStateManager,
    private val playerController: PlayerController
) : Presenter<BookmarkView>() {

  var bookId = -1L
  private val bookmarks = ArrayList<Bookmark>()
  private val chapters = ArrayList<Chapter>()

  override fun onBind(view: BookmarkView, disposables: CompositeDisposable) {
    check(bookId != -1L) { "You must initialize the bookId" }

    val book = repo.bookById(bookId) ?: return
    bookmarks.clear()
    bookmarks.addAll(bookmarkProvider.bookmarks(book))
    chapters.clear()
    chapters.addAll(book.chapters)

    renderView()
  }

  fun deleteBookmark(id: Long) {
    bookmarkProvider.deleteBookmark(id)
    bookmarks.removeAll { it.id == id }
    renderView()
  }

  fun selectBookmark(id: Long) {
    val bookmark = bookmarks.find { it.id == id }
        ?: return

    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.PLAYING

    prefs.currentBookId.value = bookId
    playerController.changePosition(bookmark.time, bookmark.mediaFile)

    if (wasPlaying) {
      playerController.play()
    }

    view.finish()
  }

  fun editBookmark(id: Long, newTitle: String) {
    bookmarks.find { it.id == id }?.let {
      val withNewTitle = it.copy(
          title = newTitle,
          id = Bookmark.ID_UNKNOWN
      )
      bookmarkProvider.deleteBookmark(it.id)
      val newBookmark = bookmarkProvider.addBookmark(withNewTitle)
      val index = bookmarks.indexOfFirst { it.id == id }
      bookmarks[index] = newBookmark
      renderView()
    }
  }

  fun addBookmark(name: String) {
    val book = repo.bookById(bookId) ?: return
    val title = if (name.isEmpty()) book.currentChapter().name else name
    val addedBookmark = bookmarkProvider.addBookmarkAtBookPosition(book, title)
    bookmarks.add(addedBookmark)
    renderView()
  }

  private fun renderView() {
    view.render(bookmarks, chapters)
  }
}
