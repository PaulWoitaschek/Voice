package voice.features.bookOverview.overview

import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.core.net.toUri
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.audiobookshelf.ServerDownloads
import voice.core.audiobookshelf.ServerLibrary
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.common.AppInfoProvider
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.core.data.Book
import voice.core.data.BookCategory
import voice.core.data.BookId
import voice.core.data.category
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.FolderType
import voice.core.data.isRemote
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.documentfile.nameWithoutExtension
import voice.core.kioskmode.DemoAudiobook
import voice.core.kioskmode.KioskModeDemoData
import voice.core.kioskmode.KioskModeFeatureFlag
import voice.core.playback.ExperimentalPlaybackPersistenceFeatureFlag
import voice.core.playback.LivePlaybackState
import voice.core.playback.PlayerController
import voice.core.playback.overlay
import voice.core.playback.playstate.PlayStateManager
import voice.core.scanner.DeviceHasStoragePermissionBug
import voice.core.scanner.MediaScanTrigger
import voice.core.ui.GridCount
import voice.core.ui.GridMode
import voice.core.ui.GridModeStore
import voice.features.bookOverview.di.BookOverviewScope
import voice.features.bookOverview.hideBook.BookHider
import voice.features.bookOverview.hideBook.HiddenBooks
import voice.features.bookOverview.survey.Survey
import voice.navigation.Destination
import voice.navigation.Navigator

@SingleIn(BookOverviewScope::class)
@Inject
class BookOverviewViewModel(
  private val repo: BookRepository,
  private val mediaScanner: MediaScanTrigger,
  private val playStateManager: PlayStateManager,
  private val playerController: PlayerController,
  private val currentBookStoreDataStore: CurrentBookStore,
  private val folderPickerMovedDialogShownStore: FolderPickerMovedDialogShownStore,
  private val gridModeStore: GridModeStore,
  private val gridCount: GridCount,
  private val navigator: Navigator,
  private val appInfoProvider: AppInfoProvider,
  private val deviceHasStoragePermissionBug: DeviceHasStoragePermissionBug,
  private val folderPickerInSettingsFeatureFlag: FolderPickerInSettingsFeatureFlag,
  private val experimentalPlaybackPersistenceFeatureFlag:
    ExperimentalPlaybackPersistenceFeatureFlag,
  private val kioskModeFeatureFlag: KioskModeFeatureFlag,
  private val dispatcherProvider: DispatcherProvider,
  private val serverLibrary: ServerLibrary,
  private val serverDownloads: ServerDownloads,
  private val audiobookFolders: AudiobookFolders,
  private val emptyLibraryTracker: EmptyLibraryTracker,
  private val survey: Survey,
  private val showHiddenBooksStore: ShowHiddenBooksStore,
  private val libraryGroupingStore: LibraryGroupingStore,
  private val showNotDownloadedBooksStore: ShowNotDownloadedBooksStore,
  private val hiddenSourcesStore: HiddenSourcesStore,
  private val bookHider: BookHider,
) {

  private val scope = MainScope(dispatcherProvider)
  private var dialog by mutableStateOf<BookOverviewViewState.Dialog?>(null)

  internal val hiddenBooks: Flow<HiddenBooks>
    get() = bookHider.hidden

  fun attach() {
    mediaScanner.scan()
    serverLibrary.sync()
  }

  @Composable
  internal fun state(): BookOverviewViewState {
    val kioskMode = remember { kioskModeFeatureFlag.get() }
    if (kioskMode) return kioskModeState()

    val playState = remember {
      playStateManager.playStateFlow
    }
      .collectAsState(initial = PlayStateManager.PlayState.Paused)
      .value
    val hasStoragePermissionBug = remember {
      deviceHasStoragePermissionBug.hasBug
    }
      .collectAsState()
      .value
    // Everything shown on the first frame stays loading until known, so the library never shows a
    // half loaded state (e.g. no hero card, or top bar icons that disappear again).
    val library = remember { library() }.collectAsState(initial = null).value
    val currentBook = remember {
      currentBookStoreDataStore.data.map(::CurrentBook)
    }
      .collectAsState(initial = null)
      .value
    val scanning = remember { mediaScanner.scannerActive }.collectAsState(initial = false).value
    val syncing = remember { serverLibrary.syncing }.collectAsState(initial = false).value
    val scannerActive = scanning || syncing
    val serverStates = rememberServerBookStates(serverLibrary, serverDownloads)
    val downloadStates = serverStates.downloads
    val folderPickerMovedDialogShown = remember {
      folderPickerMovedDialogShownStore.data
    }
      .collectAsState(initial = null)
      .value
    val gridMode = remember { gridModeStore.data }.collectAsState(initial = null).value
    val hasFoldersWithoutAccess = remember {
      audiobookFolders.withoutAccess().map { folders -> folders.values.any { it.isNotEmpty() } }
    }
      .collectAsState(initial = null)
      .value
    val hasSurvey = remember {
      survey.url().map { it != null }
    }
      .collectAsState(initial = null)
      .value
    val showHiddenBooks = remember {
      showHiddenBooksStore.data
    }
      .collectAsState(initial = null)
      .value
    val showNotDownloadedBooks = remember {
      showNotDownloadedBooksStore.data
    }
      .collectAsState(initial = null)
      .value
    val grouping = remember { libraryGroupingStore.data }.collectAsState(initial = null).value
    if (
      library == null ||
        currentBook == null ||
        folderPickerMovedDialogShown == null ||
        gridMode == null ||
        hasFoldersWithoutAccess == null ||
        hasSurvey == null ||
        showHiddenBooks == null ||
        showNotDownloadedBooks == null ||
        grouping == null ||
        // server books would pop in and out of the library while their downloads load
        (downloadStates == null && library.books.any { it.id.isRemote })
    ) {
      return BookOverviewViewState.Loading
    }
    // the current book stays, so the library keeps its controls while it plays
    fun Book.notDownloaded() =
      id.isRemote &&
        id != currentBook.id &&
        downloadStates?.get(id) !is BookDownloadState.Downloaded
    fun Book.passesHiddenFilter() = showHiddenBooks || !custom.hidden
    fun Book.passesNotDownloadedFilter() = showNotDownloadedBooks || !notDownloaded()
    // each count is what its switch changes, with the other switch as it is
    val hiddenBookCount =
      library.shownBooks.count { it.custom.hidden && it.passesNotDownloadedFilter() }
    val notDownloadedBookCount =
      library.shownBooks.count { it.notDownloaded() && it.passesHiddenFilter() }
    val books =
      remember(
        library,
        downloadStates,
        currentBook,
        showHiddenBooks,
        showNotDownloadedBooks,
      ) {
        library.shownBooks.filter { it.passesHiddenFilter() && it.passesNotDownloadedFilter() }
      }
    // a deleted book can stay the current one
    val currentBookId = currentBook.id?.takeIf { id -> books.any { it.id == id } }

    // with every book hidden, the folders still have books, so there's nothing to add
    val showEmptyLibrary = !hasStoragePermissionBug && !scannerActive && library.books.isEmpty()
    LaunchedEffect(showEmptyLibrary) {
      if (showEmptyLibrary) emptyLibraryTracker.track()
    }

    val layoutMode =
      when (gridMode) {
        GridMode.LIST -> BookOverviewLayoutMode.List
        GridMode.GRID -> BookOverviewLayoutMode.Grid
        GridMode.FOLLOW_DEVICE ->
          if (gridCount.useGridAsDefault()) {
            BookOverviewLayoutMode.Grid
          } else {
            BookOverviewLayoutMode.List
          }
      }

    val experimentalPlaybackPersistence = experimentalPlaybackPersistenceFeatureFlag.get()
    val livePlaybackState: State<LivePlaybackState?> =
      if (experimentalPlaybackPersistence && currentBookId != null) {
        remember(currentBookId) {
            playerController.livePlaybackStateFlow(currentBookId)
          }
          .collectAsState(null)
      } else {
        remember { mutableStateOf(null) }
      }

    @Composable
    fun Book.itemState() =
      itemViewState(
        currentBookId = currentBookId,
        livePlaybackState = { livePlaybackState.value },
        unavailable = id in serverStates.unavailable,
        download = downloadStates?.get(id).badge(),
      )

    val entries =
      remember(books, currentBookId) {
        // the Continue listening card shows it
        libraryEntries(books).mapValues { (_, entries) ->
          entries.filterNot { it is LibraryEntry.Single && it.book.id == currentBookId }
        }
      }
    val authors =
      remember(entries, grouping) {
        if (grouping == LibraryGrouping.Author) entries.groupedByAuthor() else emptyMap()
      }
    val inProgressCount = remember(books) { books.count { it.category == BookCategory.CURRENT } }
    return BookOverviewViewState(
      layoutMode = layoutMode,
      sections =
        entries
          .mapValues { (category, entries) ->
            authors[category]?.map { LibraryItem.Author(it.toEntryViewState(category)) }
              ?: entries.map { entry ->
                when (entry) {
                  is LibraryEntry.Single -> LibraryItem.Book(entry.book.id, entry.book.itemState())
                  is LibraryEntry.Stack -> LibraryItem.Series(entry.series.toStackViewState())
                }
              }
          }
          .filterValues { it.isNotEmpty() },
      hero = currentBookId?.let { id -> books.find { it.id == id } }?.itemState(),
      inProgressCount = inProgressCount,
      currentBookId = currentBookId,
      playButtonState =
        if (playState is PlayStateManager.PlayState.Playing) {
            BookOverviewViewState.PlayButtonState.Playing
          } else {
            BookOverviewViewState.PlayButtonState.Paused
          }
          .takeIf { currentBookId != null },
      showEmptyLibrary = showEmptyLibrary,
      showSearchIcon = library.books.isNotEmpty(),
      showFilterIcon = library.books.isNotEmpty(),
      isLoading = scannerActive,
      showStoragePermissionBugCard = hasStoragePermissionBug,
      // adding books comes first
      showSurveyCard = hasSurvey && !showEmptyLibrary,
      showFolderAccessBadge = hasFoldersWithoutAccess,
      showFolderPickerIcon =
        !folderPickerInSettingsFeatureFlag.get() &&
          !folderPickerMovedDialogShown &&
          appInfoProvider.installTime < FolderPickerMigrationInstallTimeCutoff,
      dialog = dialog,
      hiddenBookCount = hiddenBookCount,
      showHiddenBooks = showHiddenBooks,
      notDownloadedBookCount = notDownloadedBookCount,
      showNotDownloadedBooks = showNotDownloadedBooks,
      sources = library.sources,
      grouping = grouping,
    )
  }

  private fun library(): Flow<SourcedLibrary> {
    return combine(repo.flow(), sources(), hiddenSourcesStore.data) { books, sources, storedHidden
        ->
        if (sources.size < 2) return@combine SourcedLibrary(books, books, emptyList())
        val folders = sources.mapNotNull { (source) ->
          (source as? LibrarySource.Folder)?.uri?.toUri()
        }
        val sourceOf = books.associate { it.id to it.source(folders) }
        // a book whose source has no chip, like one from a removed library, always shows
        val hidden =
          sources.map { (source) -> source }.filterTo(mutableSetOf()) { it in storedHidden }
        SourcedLibrary(
          books = books,
          shownBooks = books.filter { sourceOf[it.id] !in hidden },
          sources =
            sources.map { (source, name) ->
              LibrarySourceViewState(
                source = source,
                name = name,
                shown = source !in hidden,
              )
            },
        )
      }
      .flowOn(dispatcherProvider.io)
  }

  private fun sources(): Flow<List<Pair<LibrarySource, String>>> {
    return combine(audiobookFolders.all(), serverLibrary.libraries) { folders, libraries ->
      // a book that is a folder or a file of its own can be hidden on its own
      val sourceFolders = folders[FolderType.Root].orEmpty() + folders[FolderType.Author].orEmpty()
      sourceFolders
        .map { LibrarySource.Folder(it.uri.toString()) to it.documentFile.nameWithoutExtension() }
        .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.second }) +
        libraries.map { LibrarySource.ServerLibrary(it.id) to it.name }
    }
  }

  private fun kioskModeState(): BookOverviewViewState {
    return BookOverviewViewState(
      layoutMode = BookOverviewLayoutMode.List,
      sections =
        mapOf(
          BookCategory.CURRENT to
            KioskModeDemoData.demoAudiobooks
              .filter { it.id != KioskModeDemoData.currentlyPlaying.id }
              .map { book -> LibraryItem.Book(book.id, mutableStateOf(book.toItemViewState())) },
        ),
      hero = mutableStateOf(KioskModeDemoData.currentlyPlaying.toItemViewState()),
      inProgressCount = KioskModeDemoData.demoAudiobooks.size,
      currentBookId = KioskModeDemoData.currentlyPlaying.id,
      playButtonState = BookOverviewViewState.PlayButtonState.Paused,
      showEmptyLibrary = false,
      showSearchIcon = true,
      isLoading = false,
      showStoragePermissionBugCard = false,
      showSurveyCard = false,
      showFolderAccessBadge = false,
      showFolderPickerIcon = false,
      dialog = null,
    )
  }

  fun onSettingsClick() {
    navigator.goTo(Destination.Settings)
  }

  fun onSearchClick() {
    navigator.goTo(Destination.LibrarySearch)
  }

  fun onBookClick(id: BookId) {
    navigator.goTo(Destination.Playback(id))
  }

  fun onBookLongClick(id: BookId) {
    navigator.goTo(Destination.BookActions(id))
  }

  fun onSeriesClick(name: String) {
    navigator.goTo(Destination.Series(name))
  }

  fun onSeriesLongClick(name: String) {
    navigator.goTo(Destination.SeriesActions(name))
  }

  fun onAuthorClick(name: String?) {
    navigator.goTo(Destination.Author(name))
  }

  fun onGroupingChange(grouping: LibraryGrouping) {
    scope.launch {
      libraryGroupingStore.updateData { grouping }
    }
  }

  fun onShowHiddenBooksChange(show: Boolean) {
    scope.launch {
      showHiddenBooksStore.updateData { show }
    }
  }

  fun onShowNotDownloadedBooksChange(show: Boolean) {
    scope.launch {
      showNotDownloadedBooksStore.updateData { show }
    }
  }

  fun onSourceChange(
    source: LibrarySource,
    show: Boolean,
  ) {
    scope.launch {
      hiddenSourcesStore.updateData { hidden -> if (show) hidden - source else hidden + source }
    }
  }

  internal fun onUndoHide(ids: List<BookId>) {
    scope.launch {
      bookHider.show(ids)
    }
  }

  fun onBookFolderClick() {
    dialog = BookOverviewViewState.Dialog.FolderPickerMovedToSettings
  }

  fun onFolderPickerMovedDialogDismiss() {
    dialog = null
    scope.launch {
      folderPickerMovedDialogShownStore.updateData { true }
    }
  }

  fun playPause() {
    playerController.playPause()
  }

  fun onShowFoldersClick() {
    navigator.goTo(Destination.FolderPicker)
  }

  fun onSurveyClick() {
    scope.launch {
      val url = survey.url().first() ?: return@launch
      navigator.goTo(Destination.Website(survey.open(url)))
    }
  }

  fun onSurveyDismiss() {
    scope.launch {
      val url = survey.url().first() ?: return@launch
      survey.dismiss(url)
    }
  }

  fun onPermissionBugCardClick() {
    if (Build.VERSION.SDK_INT >= 30) {
      navigator.goTo(
        Destination.Activity(
          Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
            .setData("package:com.android.externalstorage".toUri()),
        ),
      )
    }
  }
}

private val FolderPickerMigrationInstallTimeCutoff = Instant.parse("2026-06-17T00:00:00Z")

@Composable
private fun Book.itemViewState(
  currentBookId: BookId?,
  livePlaybackState: () -> LivePlaybackState?,
  unavailable: Boolean,
  download: DownloadBadge?,
): State<BookOverviewItemViewState> {
  if (id != currentBookId) {
    return rememberUpdatedState(toItemViewState(unavailable, download))
  }
  val currentPlaybackState by rememberUpdatedState(livePlaybackState)
  return remember(this, currentBookId, unavailable, download) {
    derivedStateOf {
      val livePlayback = currentPlaybackState()
      if (livePlayback != null) {
          overlay(livePlayback)
        } else {
          this
        }
        .toItemViewState(unavailable, download)
    }
  }
}

private fun DemoAudiobook.toItemViewState() =
  BookOverviewItemViewState(
    name = title,
    author = author,
    cover = coverUrl,
    progress = progress / 100F,
    id = id,
    remainingTime = remaining,
  )

private class SourcedLibrary(
  val books: List<Book>,
  val shownBooks: List<Book>,
  val sources: List<LibrarySourceViewState>,
)

/** Distinguishes "no current book" from "not loaded yet". */
internal data class CurrentBook(val id: BookId?)
