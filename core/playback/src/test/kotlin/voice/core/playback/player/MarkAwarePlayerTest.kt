package voice.core.playback.player

import android.os.Bundle
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.Runs
import io.mockk.clearMocks
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.runner.RunWith
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ChapterMark
import voice.core.data.MarkData
import voice.core.data.repo.BookRepository
import voice.core.playback.session.EXTRA_MARK_START_MS
import voice.core.playback.session.MediaId
import voice.core.playback.session.MediaItemProvider
import java.time.Instant
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Duration.Companion.milliseconds
import kotlin.uuid.Uuid

@RunWith(AndroidJUnit4::class)
class MarkAwarePlayerTest {

  private val scope = TestScope()
  private val bookId = BookId(Uuid.random().toString())
  private lateinit var currentBook: Book

  private val repoFlow = MutableStateFlow<Book?>(null)
  private val repo = mockk<BookRepository> {
    coEvery { get(bookId) } answers { currentBook }
    coEvery { updateBook(any(), any()) } just Runs
    every { flow(any()) } answers { repoFlow.filterNotNull() }
  }

  private val mediaItemProvider = mockk<MediaItemProvider>()

  // Mock player states
  private var currentPositionMock = 0L
  private var durationMock = 10_000L
  private var bufferedPositionMock = 0L
  private var isPlayingMock = false
  private var currentMediaItemMock: MediaItem? = null
  private val listeners = mutableListOf<Player.Listener>()

  private val voicePlayer = mockk<VoicePlayer>(relaxed = true) {
    every { currentMediaItemIndex } returns 0
    every { mediaItemCount } returns 1
    every { currentMediaItem } answers { currentMediaItemMock }
    every { currentPosition } answers { currentPositionMock }
    every { duration } answers { durationMock }
    every { bufferedPosition } answers { bufferedPositionMock }
    every { isPlaying } answers { isPlayingMock }
    every { getMediaItemAt(0) } answers { currentMediaItemMock ?: mockk(relaxed = true) }
    every { addListener(any()) } answers {
      listeners.add(arg(0))
    }
  }

  private lateinit var markAware: MarkAwarePlayer

  @BeforeTest
  fun setUp() {
    listeners.clear()
    currentPositionMock = 0L
    durationMock = 10_000L
    bufferedPositionMock = 0L
    isPlayingMock = false
    currentMediaItemMock = null

    // By default mock MediaItemProvider behavior
    every { mediaItemProvider.mediaItemForMark(any(), any(), any()) } answers {
      val chapter = arg<Chapter>(0)
      val mark = arg<ChapterMark>(1)
      val content = arg<BookContent>(2)
      createTestMediaItem(
        title = mark.name ?: chapter.name ?: content.name,
        mediaId = MediaId.Chapter(bookId = content.id, chapterId = chapter.id),
        album = content.name,
        extras = Bundle().apply { putLong(EXTRA_MARK_START_MS, mark.startMs) }
      )
    }

    markAware = MarkAwarePlayer(
      voicePlayer = voicePlayer,
      mediaItemProvider = mediaItemProvider,
      repo = repo,
      scope = scope,
    )
  }

  @AfterTest
  fun tearDown() {
    markAware.releaseObservers()
    scope.coroutineContext[Job]?.cancelChildren()
  }

  @Test
  fun `currentPosition is mark-relative`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = null),
          ChapterMark(startMs = 5_000, endMs = 9_999, name = null),
        ),
      ),
    )

    // Position inside second mark (5000..9999) -> mark-relative should be 2000
    currentPositionMock = 7_000L
    assertEquals(expected = 2_000L, actual = markAware.currentPosition)
    markAware.releaseObservers()
  }

  @Test
  fun `duration returns current mark duration`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = null),
          ChapterMark(startMs = 5_000, endMs = 9_999, name = null),
        ),
      ),
    )

    // Inside first mark (0..4999) -> duration is 5000
    currentPositionMock = 1_000L
    assertEquals(expected = 5_000L, actual = markAware.duration)
    markAware.releaseObservers()
  }

  @Test
  fun `seekTo translates mark-relative to file-relative`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = null),
          ChapterMark(startMs = 5_000, endMs = 9_999, name = null),
        ),
      ),
    )

    // Currently inside second mark (startMs = 5000)
    currentPositionMock = 6_000L

    // Seek to 2000 relative -> should seek to 7000 absolute
    markAware.seekTo(2_000L)
    verify { voicePlayer.seekTo(7_000L) }
    markAware.releaseObservers()
  }

  @Test
  fun `seekTo clamps mark-relative position to mark duration`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = null),
          ChapterMark(startMs = 5_000, endMs = 9_999, name = null),
        ),
      ),
    )

    // Currently inside second mark (startMs = 5000, duration = 4999)
    currentPositionMock = 6_000L

    // Seek to out-of-bounds 99_999 -> should clamp to mark duration (4999) -> seek to 9999
    markAware.seekTo(99_999L)
    verify { voicePlayer.seekTo(9_999L) }
    markAware.releaseObservers()
  }

  @Test
  fun `current MediaItem carries markStartMs in metadata extras`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = null),
          ChapterMark(startMs = 5_000, endMs = 9_999, name = null),
        ),
      ),
    )

    // Trigger position discontinuity to the second mark
    currentPositionMock = 6_000L
    clearMocks(voicePlayer, answers = false, recordedCalls = true, childMocks = false)
    triggerPositionDiscontinuity()

    verify {
      voicePlayer.replaceMediaItem(0, any())
    }
    markAware.releaseObservers()
  }

  @Test
  fun `title uses mark name when present`() = scope.runTest {
    setMediaItems(
      listOf(
        chapterWithName(
          name = "Chapter 1",
          marks = arrayOf(
            ChapterMark(startMs = 0, endMs = 4_999, name = "Intro"),
            ChapterMark(startMs = 5_000, endMs = 9_999, name = "Climax"),
          ),
        ),
      ),
    )

    currentPositionMock = 6_000L
    clearMocks(voicePlayer, answers = false, recordedCalls = true, childMocks = false)
    triggerPositionDiscontinuity()

    // Assert it replaces the item with the correct title
    val slot = slot<MediaItem>()
    verify { voicePlayer.replaceMediaItem(0, capture(slot)) }
    assertEquals(expected = "Climax", actual = slot.captured.mediaMetadata.title?.toString())
    markAware.releaseObservers()
  }

  @Test
  fun `title falls back to book name when chapter and mark names are null`() = scope.runTest {
    setMediaItems(
      listOf(
        chapterWithName(
          name = null,
          marks = emptyArray(),
        ),
      ),
    )

    currentPositionMock = 1L
    clearMocks(voicePlayer, answers = false, recordedCalls = true, childMocks = false)
    triggerPositionDiscontinuity()

    val slot = slot<MediaItem>()
    verify { voicePlayer.replaceMediaItem(0, capture(slot)) }
    assertEquals(expected = currentBook.content.name, actual = slot.captured.mediaMetadata.title?.toString())
    markAware.releaseObservers()
  }

  @Test
  fun `book rename updates current MediaItem album`() = scope.runTest {
    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 9_999, name = null))),
    )

    val renamed = currentBook.update { it.copy(name = "Renamed Book") }
    clearMocks(voicePlayer, answers = false, recordedCalls = true, childMocks = false)
    repoFlow.value = renamed
    runCurrent()

    val slot = slot<MediaItem>()
    verify(atLeast = 1) { voicePlayer.replaceMediaItem(0, capture(slot)) }
    assertEquals(expected = "Renamed Book", actual = slot.captured.mediaMetadata.albumTitle?.toString())
    markAware.releaseObservers()
  }

  @Test
  fun `returns direct values when current mark is null`() = scope.runTest {
    // No book is set, so mark is null
    assertEquals(expected = voicePlayer.duration, actual = markAware.duration)
    assertEquals(expected = voicePlayer.currentPosition, actual = markAware.currentPosition)
    assertEquals(expected = voicePlayer.bufferedPosition, actual = markAware.bufferedPosition)

    markAware.seekTo(1234L)
    verify { voicePlayer.seekTo(1234L) }
    markAware.releaseObservers()
  }

  @Test
  fun `handles C TIME_UNSET position and buffer when mark is active`() = scope.runTest {
    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 5_000, endMs = 9_999, name = null))),
    )

    currentPositionMock = C.TIME_UNSET
    assertEquals(expected = C.TIME_UNSET, actual = markAware.currentPosition)

    bufferedPositionMock = C.TIME_UNSET
    assertEquals(expected = C.TIME_UNSET, actual = markAware.bufferedPosition)
    markAware.releaseObservers()
  }

  @Test
  fun `syncCurrentBook handles transition to null mediaItem`() = scope.runTest {
    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 9_999, name = null))),
    )

    // Transition to a null / non-chapter media item
    triggerMediaItemTransition(null)

    // Since currentBook is null, duration should fall back to voicePlayer.duration
    assertEquals(expected = voicePlayer.duration, actual = markAware.duration)
    markAware.releaseObservers()
  }

  @Test
  fun `ticking periodically replaces media item when playing`() = scope.runTest {
    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = "First"),
          ChapterMark(startMs = 5_000, endMs = 9_999, name = "Second"),
        ),
      ),
    )

    isPlayingMock = true
    triggerIsPlayingChanged(true)
    runCurrent()

    // Move to next mark position
    currentPositionMock = 6_000L
    clearMocks(voicePlayer, answers = false, recordedCalls = true, childMocks = false)

    // Advance time to let ticker run
    advanceTimeBy(300.milliseconds)
    runCurrent()

    verify {
      voicePlayer.replaceMediaItem(0, any())
    }
    markAware.releaseObservers()
  }

  @Test
  fun `immediate ticking starting when player is already playing on creation`() = scope.runTest {
    isPlayingMock = true
    currentPositionMock = 6_000L

    setMediaItems(
      listOf(
        chapter(
          ChapterMark(startMs = 0, endMs = 4_999, name = "First"),
          ChapterMark(startMs = 5_000, endMs = 9_999, name = "Second"),
        ),
      ),
    )

    clearMocks(voicePlayer, answers = false, recordedCalls = true, childMocks = false)
    val newMarkAware = MarkAwarePlayer(
      voicePlayer = voicePlayer,
      mediaItemProvider = mediaItemProvider,
      repo = repo,
      scope = scope,
    )
    runCurrent()

    advanceTimeBy(300.milliseconds)
    runCurrent()

    verify {
      voicePlayer.replaceMediaItem(0, any())
    }

    newMarkAware.releaseObservers()
    markAware.releaseObservers()
  }

  @Test
  fun `releaseObservers stops all background jobs`() = scope.runTest {
    setMediaItems(
      listOf(chapter(ChapterMark(startMs = 0, endMs = 9_999, name = null))),
    )

    markAware.releaseObservers()
    clearMocks(voicePlayer, answers = false, recordedCalls = true, childMocks = false)

    val renamed = currentBook.update { it.copy(name = "Never Collected") }
    repoFlow.value = renamed
    runCurrent()

    // Should not trigger replaceMediaItem after release
    verify(exactly = 0) { voicePlayer.replaceMediaItem(any(), any()) }
  }

  private fun createTestMediaItem(
    title: String,
    mediaId: MediaId,
    album: String? = null,
    extras: Bundle? = null,
  ): MediaItem {
    val mediaIdString = kotlinx.serialization.json.Json.encodeToString(MediaId.serializer(), mediaId)
    val metadata = androidx.media3.common.MediaMetadata.Builder()
      .setTitle(title)
      .setAlbumTitle(album)
      .setExtras(extras)
      .build()
    return MediaItem.Builder()
      .setMediaId(mediaIdString)
      .setMediaMetadata(metadata)
      .build()
  }

  private fun localBook(
    chapters: List<Chapter>,
    id: BookId = BookId(Uuid.random().toString()),
    positionInChapter: Long = 0,
  ): Book {
    return Book(
      content = BookContent(
        author = Uuid.random().toString(),
        name = Uuid.random().toString(),
        positionInChapter = positionInChapter,
        playbackSpeed = 1F,
        addedAt = Instant.EPOCH,
        chapters = chapters.map { it.id },
        cover = null,
        currentChapter = chapters.first().id,
        isActive = true,
        lastPlayedAt = Instant.EPOCH,
        skipSilence = false,
        id = id,
        gain = 0F,
        genre = null,
        narrator = null,
        series = null,
        part = null,
      ),
      chapters = chapters,
    )
  }

  private fun TestScope.setMediaItems(
    chapters: List<Chapter>,
  ) {
    currentBook = localBook(chapters, bookId)
    repoFlow.value = currentBook
    val mediaItem = createTestMediaItem(
      title = chapters.first().name ?: "chapter",
      mediaId = MediaId.Chapter(bookId = bookId, chapterId = chapters.first().id)
    )
    currentMediaItemMock = mediaItem
    triggerMediaItemTransition(mediaItem)
    runCurrent()
  }

  private fun chapter(vararg marks: ChapterMark): Chapter = chapterWithName(name = "chapter", marks = marks)

  private fun chapterWithName(
    name: String?,
    marks: Array<out ChapterMark>,
  ): Chapter {
    val duration = if (marks.isEmpty()) 9_999L else marks.maxOf { it.endMs }
    return Chapter(
      id = ChapterId(Uuid.random().toString()),
      name = name,
      duration = duration,
      fileLastModified = Instant.EPOCH,
      markData = marks.map { MarkData(it.startMs, it.name ?: "mark ") },
      fileSize = 0,
    )
  }

  private fun triggerMediaItemTransition(mediaItem: MediaItem?) {
    listeners.forEach { it.onMediaItemTransition(mediaItem, Player.MEDIA_ITEM_TRANSITION_REASON_PLAYLIST_CHANGED) }
  }

  private fun triggerPositionDiscontinuity() {
    listeners.forEach {
      it.onPositionDiscontinuity(
        mockk(relaxed = true),
        mockk(relaxed = true),
        Player.DISCONTINUITY_REASON_SEEK,
      )
    }
  }

  private fun triggerIsPlayingChanged(isPlaying: Boolean) {
    listeners.forEach { it.onIsPlayingChanged(isPlaying) }
  }
}
