package voice.features.widget

import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.toArgb
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.shareIn
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.repo.BookContentRepo
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.CurrentBookResolver
import voice.core.playback.LastPlaybackSpeedStore
import voice.core.playback.playstate.PlayStateManager
import voice.core.playback.withDefaultSpeed
import voice.core.sleeptimer.SleepTimer
import voice.core.ui.CoverColorsStore
import voice.core.ui.CoverSeedColors
import voice.core.ui.ThemeColorSchemeStore
import java.time.Instant
import kotlin.time.Duration.Companion.minutes

/**
 * What the widgets show. The models are cheap and only change when what the widgets show does, so
 * they decide when widgets update. Covers and colors are only loaded for the widgets themselves.
 */
@SingleIn(AppScope::class)
@Inject
class WidgetData(
  private val bookRepository: BookRepository,
  private val bookContentRepo: BookContentRepo,
  private val currentBookResolver: Lazy<CurrentBookResolver>,
  private val currentBookStore: CurrentBookStore,
  private val lastPlaybackSpeedStore: LastPlaybackSpeedStore,
  private val playStateManager: PlayStateManager,
  private val sleepTimer: SleepTimer,
  private val themeColorSchemeStore: ThemeColorSchemeStore,
  private val coverColorsStore: CoverColorsStore,
  private val coverSeedColors: CoverSeedColors,
  private val images: WidgetImages,
  scope: CoroutineScope,
) {

  private val sharing = SharingStarted.WhileSubscribed(replayExpirationMillis = 0)

  private val playing: Flow<Boolean> = playStateManager.playStateFlow
    .map { it == PlayStateManager.PlayState.Playing }
    .distinctUntilChanged()

  private val currentBookId: Flow<BookId?> = currentBookStore.data.distinctUntilChanged()

  private val colorSettings: Flow<WidgetColorSettings> = combine(
    themeColorSchemeStore.data,
    coverColorsStore.data,
    ::WidgetColorSettings,
  )

  private val currentBook: Flow<Book?> = currentBookId
    .flatMapLatest { id ->
      if (id == null) {
        flowOf(null)
      } else {
        combine(
          bookRepository.flow(id).distinctUntilChangedBy { it?.content?.withoutPlaybackNoise() },
          playing,
          refreshWhilePlaying(),
          lastPlaybackSpeedStore.data,
        ) { book, playing, _, defaultSpeed ->
          if (book != null && playing) {
            // with live playback persistence, the database doesn't follow the position while playing
            currentBookResolver.value.book(id) ?: book
          } else {
            book
          }?.withDefaultSpeed { defaultSpeed }
        }
      }
    }
    .shareIn(scope, sharing, replay = 1)

  internal val nowPlaying: Flow<NowPlayingModel> = combine(
    currentBook,
    playing,
    sleepTimer.state,
    colorSettings,
    libraryEmpty(),
  ) { book, playing, sleepTimerState, colorSettings, libraryEmpty ->
    NowPlayingModel(
      book = book?.let { nowPlayingBook(it, playing, sleepTimerState, Instant.now()) },
      libraryEmpty = libraryEmpty,
      colorSettings = colorSettings,
    )
  }
    .distinctUntilChanged()
    .shareIn(scope, sharing, replay = 1)

  internal val shelf: Flow<ShelfModel> = combine(
    library(),
    currentBookId,
    playing,
    colorSettings,
  ) { books, currentBookId, playing, colorSettings ->
    ShelfModel(
      books = shelfBooks(books, currentBookId, playing),
      colorSettings = colorSettings,
    )
  }
    .distinctUntilChanged()
    .shareIn(scope, sharing, replay = 1)

  internal fun nowPlayingState(): Flow<NowPlayingState> = nowPlaying.mapLatest { model ->
    val theme = theme(model.book?.cover, model.colorSettings)
    if (model.book == null) {
      NowPlayingState.Empty(libraryEmpty = model.libraryEmpty, theme = theme)
    } else {
      NowPlayingState.Current(book = model.book, cover = images.cover(model.book.cover), theme = theme)
    }
  }

  internal fun shelfState(): Flow<ShelfState> = shelf.mapLatest { model ->
    ShelfState(
      books = model.books.map { ShelfItem(it, images.cover(it.cover)) },
      theme = theme(model.books.firstOrNull()?.cover, model.colorSettings),
    )
  }

  private suspend fun theme(
    cover: String?,
    colorSettings: WidgetColorSettings,
  ): WidgetTheme {
    val seed = cover?.takeIf { colorSettings.coverColors }
      ?.let { coverSeedColors.load(it) }
      ?.takeIf { it.isSpecified }
    return WidgetTheme(seed = seed?.toArgb(), themeColorScheme = colorSettings.themeColorScheme)
  }

  private fun libraryEmpty(): Flow<Boolean> = currentBookId
    .flatMapLatest { id ->
      // only an empty widget asks, so the whole library isn't followed while a book plays
      if (id == null) {
        bookRepository.flow().map { it.isEmpty() }
      } else {
        flowOf(false)
      }
    }
    .distinctUntilChanged()

  // building every book of the library for each position save would be wasted on the shelf. Books
  // that weren't saved keep their instance, so only the one that plays is compared field by field.
  private fun library(): Flow<List<Book>> = bookContentRepo.flow()
    .distinctUntilChanged { old, new ->
      old.size == new.size &&
        old.indices.all { old[it] === new[it] || old[it].withoutPlaybackNoise() == new[it].withoutPlaybackNoise() }
    }
    .map { bookRepository.all() }

  private fun refreshWhilePlaying(): Flow<Int> = playing.flatMapLatest { playing ->
    flow {
      var tick = 0
      emit(tick)
      while (playing) {
        delay(1.minutes)
        emit(++tick)
      }
    }
  }
}

// the position is saved several times a second while playing, but the widgets only show minutes
private fun BookContent.withoutPlaybackNoise(): BookContent = copy(
  positionInChapter = positionInChapter / 60_000,
  // whether the book was played stays, because a book that never was shows the last speed
  lastPlayedAt = if (lastPlayedAt == Instant.EPOCH) Instant.EPOCH else Instant.MAX,
)
