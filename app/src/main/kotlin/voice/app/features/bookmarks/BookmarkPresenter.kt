package voice.app.features.bookmarks

import androidx.datastore.core.DataStore
import kotlinx.coroutines.launch
import voice.app.mvp.Presenter
import voice.common.pref.CurrentBook
import voice.data.Book
import voice.data.Bookmark
import voice.data.Chapter
import voice.data.repo.BookRepository
import voice.data.repo.BookmarkRepo
import voice.playback.PlayerController
import voice.playback.playstate.PlayStateManager
import javax.inject.Inject


class BookmarkPresenter
@Inject constructor(
  @CurrentBook
  private val currentBook: DataStore<Book.Id?>,
  private val repo: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController
) : Presenter<BookmarkView>() {

  lateinit var bookId: Book.Id
  private val bookmarks = ArrayList<Bookmark>()
  private val chapters = ArrayList<Chapter>()

  override fun onAttach(view: BookmarkView) {
    onAttachScope.launch {
      val book = repo.get(bookId) ?: return@launch
      bookmarks.clear()
      bookmarks.addAll(
        bookmarkRepo.bookmarks(book.content)
          .sortedByDescending { it.addedAt }
      )
      chapters.clear()
      chapters.addAll(book.chapters)

      renderView()
    }
  }

  fun deleteBookmark(id: Bookmark.Id) {
    scope.launch {
      bookmarkRepo.deleteBookmark(id)
      bookmarks.removeAll { it.id == id }

      renderView()
    }
  }

  fun selectBookmark(id: Bookmark.Id) {
    val bookmark = bookmarks.find { it.id == id }
      ?: return

    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.Playing

    scope.launch {
      currentBook.updateData { bookId }
    }
    playerController.setPosition(bookmark.time, bookmark.chapterId)

    if (wasPlaying) {
      playerController.play()
    }

    view.finish()
  }

  fun editBookmark(id: Bookmark.Id, newTitle: String) {
    scope.launch {
      bookmarks.find { it.id == id }?.let {
        val withNewTitle = it.copy(
          title = newTitle,
          setBySleepTimer = false,
        )
        bookmarkRepo.addBookmark(withNewTitle)
        val index = bookmarks.indexOfFirst { bookmarkId -> bookmarkId.id == id }
        bookmarks[index] = withNewTitle
        if (attached) renderView()
      }
    }
  }

  fun addBookmark(name: String) {
    scope.launch {
      val book = repo.get(bookId) ?: return@launch
      val addedBookmark = bookmarkRepo.addBookmarkAtBookPosition(
        book = book,
        title = name,
        setBySleepTimer = false
      )
      bookmarks.add(addedBookmark)
      if (attached) renderView()
    }
  }

  private fun renderView() {
    if (attached) {
      view.render(bookmarks, chapters)
    }
  }
}
