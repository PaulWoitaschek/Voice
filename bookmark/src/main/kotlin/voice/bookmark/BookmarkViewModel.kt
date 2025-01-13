package voice.bookmark

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.datastore.core.DataStore
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import voice.common.BookId
import voice.common.formatTime
import voice.common.navigation.Navigator
import voice.common.pref.CurrentBook
import voice.data.Bookmark
import voice.data.Chapter
import voice.data.markForPosition
import voice.data.repo.BookRepository
import voice.data.repo.BookmarkRepo
import voice.playback.PlayerController
import voice.playback.playstate.PlayStateManager
import voice.strings.R
import java.time.Instant
import java.time.temporal.ChronoUnit
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes

class BookmarkViewModel
@AssistedInject constructor(
  @CurrentBook
  private val currentBook: DataStore<BookId?>,
  private val repo: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  private val navigator: Navigator,
  private val context: Context,
  @Assisted
  private val bookId: BookId,
) {

  private val scope = MainScope()
  private var bookmarks by mutableStateOf<List<Bookmark>>(emptyList())
  private var chapters by mutableStateOf<List<Chapter>>(emptyList())

  private var shouldScrollTo by mutableStateOf<Bookmark.Id?>(null)
  private var dialogViewState: BookmarkDialogViewState by mutableStateOf(BookmarkDialogViewState.None)

  @Composable
  fun viewState(): BookmarkViewState {
    LaunchedEffect(bookId) {
      val book = repo.get(bookId)
      if (book != null) {
        bookmarks = bookmarkRepo.bookmarks(book.content)
          .sortedByDescending { it.addedAt }
        chapters = book.chapters
      }
    }
    return BookmarkViewState(
      bookmarks = bookmarks.map { bookmark ->
        val currentChapter = chapters.single { it.id == bookmark.chapterId }
        val bookmarkTitle = bookmark.title
        val title: String = when {
          bookmark.setBySleepTimer -> {
            val justNowThreshold = 1.minutes
            if (ChronoUnit.MILLIS.between(bookmark.addedAt, Instant.now()).milliseconds < justNowThreshold) {
              context.getString(R.string.bookmark_just_now)
            } else {
              DateUtils.getRelativeDateTimeString(
                context,
                bookmark.addedAt.toEpochMilli(),
                justNowThreshold.inWholeMilliseconds,
                2.days.inWholeMilliseconds,
                0,
              ).toString()
            }
          }
          !bookmarkTitle.isNullOrEmpty() -> bookmarkTitle
          else -> currentChapter.markForPosition(bookmark.time).name ?: ""
        }

        BookmarkItemViewState(
          title = title,
          subtitle = formatTime(bookmark.time),
          id = bookmark.id,
          showSleepIcon = bookmark.setBySleepTimer,
        )
      },
      shouldScrollTo = shouldScrollTo,
      dialogViewState = dialogViewState,
    )
  }

  fun deleteBookmark(id: Bookmark.Id) {
    scope.launch {
      bookmarkRepo.deleteBookmark(id)
      bookmarks = bookmarks.filter { it.id != id }
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

    navigator.goBack()
  }

  fun editBookmark(
    id: Bookmark.Id,
    newTitle: String,
  ) {
    scope.launch {
      bookmarks.find { it.id == id }?.let {
        val withNewTitle = it.copy(
          title = newTitle,
          setBySleepTimer = false,
        )
        bookmarkRepo.addBookmark(withNewTitle)
        val index = bookmarks.indexOfFirst { bookmarkId -> bookmarkId.id == id }
        bookmarks = bookmarks.toMutableList().apply {
          this[index] = withNewTitle
        }
      }
    }
  }

  fun addBookmark(name: String) {
    scope.launch {
      val book = repo.get(bookId) ?: return@launch
      val newBookmark = bookmarkRepo.addBookmarkAtBookPosition(
        book = book,
        title = name,
        setBySleepTimer = false,
      )
      bookmarks = (bookmarks + newBookmark)
        .sortedByDescending { it.addedAt }
      shouldScrollTo = newBookmark.id
    }
  }

  fun onScrollConfirm() {
    shouldScrollTo = null
  }

  fun closeDialog() {
    dialogViewState = BookmarkDialogViewState.None
  }

  fun onAddClick() {
    dialogViewState = BookmarkDialogViewState.AddBookmark
  }

  fun onEditClick(id: Bookmark.Id) {
    val bookmark = bookmarks.find { it.id == id } ?: return
    dialogViewState = BookmarkDialogViewState.EditBookmark(id, bookmark.title)
  }

  fun closeScreen() {
    navigator.goBack()
  }

  @AssistedFactory
  interface Factory {
    fun create(id: BookId): BookmarkViewModel
  }
}
