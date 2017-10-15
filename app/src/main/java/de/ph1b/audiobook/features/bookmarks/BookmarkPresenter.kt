package de.ph1b.audiobook.features.bookmarks

import de.ph1b.audiobook.Bookmark
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.injection.PrefKeys
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.persistence.BookRepository
import de.ph1b.audiobook.persistence.BookmarkRepo
import de.ph1b.audiobook.persistence.pref.Pref
import de.ph1b.audiobook.playback.PlayStateManager
import de.ph1b.audiobook.playback.PlayerController
import javax.inject.Inject
import javax.inject.Named

/**
 * Presenter for the bookmark MVP
 */
class BookmarkPresenter @Inject constructor(
    @Named(PrefKeys.CURRENT_BOOK)
    private val currentBookIdPref: Pref<Long>,
    private val repo: BookRepository,
    private val bookmarkRepo: BookmarkRepo,
    private val playStateManager: PlayStateManager,
    private val playerController: PlayerController
) : Presenter<BookmarkView>() {

  var bookId = -1L
  private val bookmarks = ArrayList<Bookmark>()
  private val chapters = ArrayList<Chapter>()

  override fun onAttach(view: BookmarkView) {
    check(bookId != -1L) { "You must initialize the bookId" }

    val book = repo.bookById(bookId) ?: return
    bookmarks.clear()
    bookmarks.addAll(bookmarkRepo.bookmarks(book))
    chapters.clear()
    chapters.addAll(book.chapters)

    renderView()
  }

  fun deleteBookmark(id: Long) {
    bookmarkRepo.deleteBookmark(id)
    bookmarks.removeAll { it.id == id }
    renderView()
  }

  fun selectBookmark(id: Long) {
    val bookmark = bookmarks.find { it.id == id }
        ?: return

    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.PLAYING

    currentBookIdPref.value = bookId
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
      bookmarkRepo.deleteBookmark(it.id)
      val newBookmark = bookmarkRepo.addBookmark(withNewTitle)
      val index = bookmarks.indexOfFirst { it.id == id }
      bookmarks[index] = newBookmark
      renderView()
    }
  }

  fun addBookmark(name: String) {
    val book = repo.bookById(bookId) ?: return
    val title = if (name.isEmpty()) book.currentChapter().name else name
    val addedBookmark = bookmarkRepo.addBookmarkAtBookPosition(book, title)
    bookmarks.add(addedBookmark)
    renderView()
  }

  private fun renderView() {
    view.render(bookmarks, chapters)
  }
}
