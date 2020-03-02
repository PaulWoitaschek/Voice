package de.ph1b.audiobook.features.bookmarks

import de.ph1b.audiobook.data.Bookmark
import de.ph1b.audiobook.data.Chapter
import de.ph1b.audiobook.data.repo.BookRepository
import de.ph1b.audiobook.data.repo.BookmarkRepo
import de.ph1b.audiobook.mvp.Presenter
import de.ph1b.audiobook.playback.PlayerController
import de.ph1b.audiobook.playback.playstate.PlayStateManager
import de.ph1b.audiobook.prefs.Pref
import de.ph1b.audiobook.prefs.PrefKeys
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject
import javax.inject.Named

/**
 * Presenter for the bookmark MVP
 */
class BookmarkPresenter @Inject constructor(
  @Named(PrefKeys.CURRENT_BOOK)
  private val currentBookIdPref: Pref<UUID>,
  private val repo: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController
) : Presenter<BookmarkView>() {

  var bookId: UUID = UUID.randomUUID()
  private val bookmarks = ArrayList<Bookmark>()
  private val chapters = ArrayList<Chapter>()

  override fun onAttach(view: BookmarkView) {
    val book = repo.bookByIdBlocking(bookId) ?: return

    GlobalScope.launch(Dispatchers.Main) {
      bookmarks.clear()
      bookmarks.addAll(bookmarkRepo.bookmarks(book))
      chapters.clear()
      chapters.addAll(book.content.chapters)

      if (attached) renderView()
    }
  }

  fun deleteBookmark(id: Long) {
    GlobalScope.launch(Dispatchers.Main) {
      bookmarkRepo.deleteBookmark(id)
      bookmarks.removeAll { it.id == id }

      renderView()
    }
  }

  fun selectBookmark(id: Long) {
    val bookmark = bookmarks.find { it.id == id }
      ?: return

    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.Playing

    currentBookIdPref.value = bookId
    playerController.setPosition(bookmark.time, bookmark.mediaFile)

    if (wasPlaying) {
      playerController.play()
    }

    view.finish()
  }

  fun editBookmark(id: Long, newTitle: String) {
    GlobalScope.launch(Dispatchers.Main) {
      bookmarks.find { it.id == id }?.let {
        val withNewTitle = it.copy(
          title = newTitle,
          id = Bookmark.ID_UNKNOWN
        )
        bookmarkRepo.deleteBookmark(it.id)
        val newBookmark = bookmarkRepo.addBookmark(withNewTitle)
        val index = bookmarks.indexOfFirst { bookmarkId -> bookmarkId.id == id }
        bookmarks[index] = newBookmark
        if (attached) renderView()
      }
    }
  }

  fun addBookmark(name: String) {
    GlobalScope.launch(Dispatchers.Main) {
      val book = repo.bookByIdBlocking(bookId) ?: return@launch
      val title = if (name.isEmpty()) book.content.currentChapter.name else name
      val addedBookmark = bookmarkRepo.addBookmarkAtBookPosition(book, title)
      bookmarks.add(addedBookmark)
      if (attached) renderView()
    }
  }

  private fun renderView() {
    view.render(bookmarks, chapters)
  }
}
