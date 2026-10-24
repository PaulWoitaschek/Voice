package voice.features.bookmark

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.Bookmark
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.data.store.CurrentBookStore
import voice.core.featureflag.FeatureFlag
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.playback.CurrentBookResolver
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.LastPlaybackSpeedStore
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.withDefaultSpeed
import voice.core.sleeptimer.SleepTimer
import voice.navigation.Navigator
import java.time.Clock
import kotlin.time.Duration.Companion.seconds

@AssistedInject
class BookmarkViewModel(
  private val currentBookStore: CurrentBookStore,
  private val bookRepository: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  private val currentBookResolver: CurrentBookResolver,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  private val sleepTimer: SleepTimer,
  private val navigator: Navigator,
  private val clock: Clock,
  dispatcherProvider: DispatcherProvider,
  private val kioskModeFeatureFlag: KioskModeFeatureFlag,
  private val experimentalPlaybackPersistenceFeatureFlag: ExperimentalPlaybackPersistenceFeatureFlag,
  private val lastPlaybackSpeedStore: LastPlaybackSpeedStore,
  private val historyTabOpenStore: HistoryTabOpenStore,
  @Assisted
  private val bookId: BookId,
  @Assisted
  private val editBookmarkId: String?,
) {

  private val scope = MainScope(dispatcherProvider)

  // buffered, so that no undo gets lost while the screen still shows the snackbar of an earlier one
  private val viewEffectChannel = Channel<BookmarkViewEffect>(Channel.UNLIMITED)
  internal val viewEffects: Flow<BookmarkViewEffect> = viewEffectChannel.receiveAsFlow()

  private var sort by mutableStateOf(BookmarkSort.Recent)
  private var selectedCategory by mutableStateOf<Bookmark.Kind?>(null)
  private var draft by mutableStateOf<Draft?>(null)

  // only once the listener picks a tab here, so that until then it follows the stored one
  private var pickedTab by mutableStateOf<BookmarkTab?>(null)
  private var saving = false

  // a jump or close leaves the screen, so a second tap must not go back once more
  private var leaving = false

  private var book: Book? = null
  private var bookmarks: List<Bookmark> = emptyList()

  @Composable
  internal fun viewState(): BookmarkViewState? {
    val kioskMode = remember { kioskModeFeatureFlag.get() }
    if (kioskMode) return kioskModeBookmarkViewState(tab = pickedTab ?: BookmarkTab.Bookmarks)

    // saved, so that the editor does not open again once the screen comes back after process death
    var openedEditorFromNavigation by rememberSaveable { mutableStateOf(false) }
    val defaultSpeed = remember { lastPlaybackSpeedStore.data }.collectAsState(initial = null).value
    val historyTabOpen = remember { historyTabOpenStore.data }.collectAsState(initial = null).value ?: return null
    val persistedBook = remember(bookId) { bookRepository.flow(bookId) }
      .collectAsState(initial = null).value
      ?.withDefaultSpeed { defaultSpeed ?: return null }
      ?: return null
    // with the experimental playback persistence, the stored position lags behind while playing
    val experimentalPlaybackPersistence = remember { experimentalPlaybackPersistenceFeatureFlag.get() }
    val livePlaybackState = if (experimentalPlaybackPersistence) {
      remember(bookId) { playerController.livePlaybackStateFlow(bookId) }
        .collectAsState(initial = null).value
    } else {
      null
    }
    val book = if (livePlaybackState != null) persistedBook.overlay(livePlaybackState) else persistedBook
    val bookmarks = remember(book.id, book.content.chapters) {
      bookmarkRepo.bookmarksFlow(book.content)
    }.collectAsState(initial = null).value ?: return null
    this.book = book
    this.bookmarks = bookmarks

    LaunchedEffect(Unit) {
      if (!openedEditorFromNavigation && editBookmarkId != null) {
        openedEditorFromNavigation = true
        bookmarks.find { it.id.value.toString() == editBookmarkId }?.let { edit(it, isNew = false) }
      }
    }

    val playState by remember { playStateManager.playStateFlow }.collectAsState()
    val sleepTimerState by remember { sleepTimer.state }.collectAsState()
    val index = remember(book) { BookIndex(book) }
    val category = selectedCategory?.takeIf { selected -> bookmarks.any { it.kind == selected } }
    val visible = if (category == null) bookmarks else bookmarks.filter { it.kind == category }
    val now = clock.instant()
    return BookmarkViewState(
      bookTitle = book.content.name,
      bookAuthor = book.content.metadata.author,
      cover = book.content.coverUrl,
      playing = livePlaybackState?.isPlaying ?: (playState == PlayStateManager.PlayState.Playing),
      sleepTimerActive = sleepTimerState.enabled,
      bookBar = bookBar(index, visible, book.content.playbackSpeed),
      sort = sort,
      categories = categoryCounts(bookmarks),
      selectedCategory = category,
      items = bookmarkListItems(index, visible, sort, now, clock.zone),
      totalCount = bookmarks.size,
      editor = draft?.let { editorViewState(index, it) },
      tab = pickedTab ?: when {
        // the bookmark to edit is in the list of bookmarks
        editBookmarkId != null -> BookmarkTab.Bookmarks
        historyTabOpen -> BookmarkTab.History
        else -> BookmarkTab.Bookmarks
      },
    )
  }

  private fun editorViewState(
    index: BookIndex,
    draft: Draft,
  ): BookmarkEditorViewState? {
    val location = index.locate(draft.bookmark.chapterId, draft.time) ?: return null
    val chapterDuration = index.chapterDuration(draft.bookmark.chapterId) ?: 0L
    return BookmarkEditorViewState(
      id = draft.bookmark.id,
      isNew = draft.isNew,
      note = draft.note,
      kind = draft.kind,
      chapterNumber = location.chapterNumber,
      chapterName = location.chapterName,
      showChapter = index.chapterCount > 1,
      time = location.time,
      percent = location.percent,
      canMoveEarlier = draft.time > 0,
      canMoveLater = draft.time < chapterDuration,
    )
  }

  internal fun onTabChange(tab: BookmarkTab) {
    pickedTab = tab
    scope.launch {
      historyTabOpenStore.updateData { tab == BookmarkTab.History }
    }
  }

  internal fun onSortChange(sort: BookmarkSort) {
    this.sort = sort
  }

  internal fun onCategoryClick(category: Bookmark.Kind?) {
    selectedCategory = if (selectedCategory == category) null else category
  }

  fun onBookmarkClick(id: Bookmark.Id) {
    if (leaving) return
    val bookmark = bookmarks.find { it.id == id } ?: return
    leaving = true
    val wasPlaying = playStateManager.playState == PlayStateManager.PlayState.Playing
    scope.launch {
      currentBookStore.updateData { bookId }
      playerController.setPosition(bookmark.time, bookmark.chapterId, ListeningEvent.Type.BookmarkJump)
      if (wasPlaying) {
        playerController.play()
      }
      navigator.goBack()
    }
  }

  fun onBookmarkLongClick(id: Bookmark.Id) {
    val bookmark = bookmarks.find { it.id == id } ?: return
    edit(bookmark, isNew = false)
  }

  /** Saves the current position right away, the sheet only offers to add details. */
  fun onSaveClick() {
    // a double tap would save the moment twice
    if (saving || draft != null) return
    saving = true
    scope.launch {
      try {
        val book = currentBookResolver.book(bookId) ?: return@launch
        val bookmark = bookmarkRepo.addBookmarkAtBookPosition(
          book = book,
          title = null,
        )
        // a filter for another kind would hide the new bookmark
        selectedCategory = null
        edit(bookmark, isNew = true)
      } finally {
        saving = false
      }
    }
  }

  internal fun onNoteChange(note: String) {
    draft = draft?.copy(note = note)
  }

  internal fun onKindChange(kind: Bookmark.Kind) {
    draft = draft?.copy(kind = kind)
  }

  internal fun onMoveEarlier() {
    moveBy(-NUDGE.inWholeMilliseconds)
  }

  internal fun onMoveLater() {
    moveBy(NUDGE.inWholeMilliseconds)
  }

  private fun moveBy(offset: Long) {
    val draft = draft ?: return
    val book = book ?: return
    val chapterDuration = BookIndex(book).chapterDuration(draft.bookmark.chapterId) ?: return
    this.draft = draft.copy(time = (draft.time + offset).coerceIn(0L, chapterDuration))
  }

  /** Done, or the sheet was swiped away: either way the changes are kept. */
  internal fun onEditorDone() {
    val draft = draft ?: return
    this.draft = null
    val updated = draft.bookmark.copy(
      title = draft.note.trim().takeIf { it.isNotEmpty() },
      kind = draft.kind,
      time = draft.time,
    )
    if (updated != draft.bookmark) {
      scope.launch {
        bookmarkRepo.addBookmark(updated)
      }
    }
  }

  internal fun onEditorUndo() {
    val draft = draft ?: return
    this.draft = null
    scope.launch {
      bookmarkRepo.deleteBookmark(draft.bookmark.id)
    }
  }

  internal fun onEditorDelete() {
    val draft = draft ?: return
    this.draft = null
    delete(draft.bookmark)
  }

  fun onDelete(id: Bookmark.Id) {
    val bookmark = bookmarks.find { it.id == id } ?: return
    delete(bookmark)
  }

  private fun delete(bookmark: Bookmark) {
    scope.launch {
      bookmarkRepo.deleteBookmark(bookmark.id)
      viewEffectChannel.send(BookmarkViewEffect.Deleted(bookmark))
    }
  }

  internal fun onUndoDelete(bookmark: Bookmark) {
    scope.launch {
      bookmarkRepo.addBookmark(bookmark)
    }
  }

  fun onCloseClick() {
    if (leaving) return
    leaving = true
    navigator.goBack()
  }

  private fun edit(
    bookmark: Bookmark,
    isNew: Boolean,
  ) {
    draft = Draft(
      bookmark = bookmark,
      isNew = isNew,
      note = bookmark.title.orEmpty(),
      kind = bookmark.kind,
      time = bookmark.time,
    )
  }

  private data class Draft(
    val bookmark: Bookmark,
    val isNew: Boolean,
    val note: String,
    val kind: Bookmark.Kind,
    val time: Long,
  )

  @AssistedFactory
  interface Factory {
    fun create(
      bookId: BookId,
      editBookmarkId: String?,
    ): BookmarkViewModel
  }
}

internal val NUDGE = 15.seconds
