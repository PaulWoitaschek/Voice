package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.common.Timeline
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.data.Book
import voice.core.data.BookContent
import voice.core.data.ChapterMark
import voice.core.data.durationMs
import voice.core.logging.api.Logger
import voice.core.playback.di.PlaybackScope
import voice.core.playback.misc.Decibel
import voice.core.playback.misc.VolumeGain
import voice.core.playback.session.ChapterMarkPlaylist
import voice.core.playback.session.MediaItemProvider
import voice.core.playback.session.chapterMarkPlaylist
import voice.core.playback.session.playbackItems

/**
 * Presents the wrapped player, which holds one media item per audio file, as a playlist with one
 * media item per chapter mark.
 *
 * A single file book is therefore extracted once instead of once per chapter, while the session,
 * the notification and the UI still see chapters as individual playlist entries.
 */
@Inject
@SingleIn(PlaybackScope::class)
class ChapterMarkPlayer(
  private val player: Player,
  private val mediaItemProvider: MediaItemProvider,
  private val volumeGain: VolumeGain,
) : ForwardingSimpleBasePlayer(player) {

  private var book: Book? = null
  private var playlist: ChapterMarkPlaylist? = null
  private var markItems: List<MediaItemData> = emptyList()
  private var reportedItemIndex: Int? = null
  private val boundaryMessages = mutableListOf<PlayerMessage>()

  init {
    player.addListener(
      object : Player.Listener {
        override fun onTimelineChanged(
          timeline: Timeline,
          reason: Int,
        ) {
          adoptResolvedDurations(timeline)
        }
      },
    )
  }

  /**
   * Plays the chapter marks of a book. [MediaItemProvider.playbackItems] tags them with their book, which tells how the
   * marks are spread over the book's files.
   */
  override fun handleSetMediaItems(
    mediaItems: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
  ): ListenableFuture<*> {
    if (mediaItems.isEmpty()) {
      clear()
      return Futures.immediateVoidFuture()
    }
    val book = mediaItems.first().localConfiguration?.tag as? Book
    if (book == null || mediaItems.size != book.playbackItems().size) {
      Logger.w("Ignoring media items that are not the chapter marks of a book")
      return Futures.immediateVoidFuture()
    }
    restoreSettings(book.content)
    // keeping the position of the previous book can point past the end of this one
    val startsAtItem = startIndex in mediaItems.indices
    setBook(
      book = book,
      markMediaItems = mediaItems,
      startItemIndex = if (startsAtItem) startIndex else 0,
      positionInItemMs = if (startsAtItem && startPositionMs != C.TIME_UNSET) startPositionMs else 0,
    )
    return Futures.immediateVoidFuture()
  }

  private fun restoreSettings(content: BookContent) {
    player.setPlaybackSpeed(content.playbackSpeed)
    setSkipSilenceEnabled(content.skipSilence)
    volumeGain.gain = Decibel(content.gain)
  }

  private fun setBook(
    book: Book,
    markMediaItems: List<MediaItem>,
    startItemIndex: Int,
    positionInItemMs: Long,
  ) {
    val playlist = applyBook(book, markMediaItems)
    val mark = playlist.items[startItemIndex].mark
    reportedItemIndex = startItemIndex
    player.setMediaItems(
      mediaItemProvider.chapterMediaItems(book),
      playlist.fileIndexOf(startItemIndex),
      mark.startMs + positionInItemMs.coerceIn(0L, mark.durationMs),
    )
    registerBoundaryMessages(playlist)
    invalidateState()
  }

  private fun applyBook(
    book: Book,
    markMediaItems: List<MediaItem> = mediaItemProvider.playbackItems(book),
  ): ChapterMarkPlaylist {
    clearBoundaryMessages()
    val playlist = book.chapterMarkPlaylist()
    this.book = book
    this.playlist = playlist
    markItems = playlist.items.mapIndexed { index, item ->
      // Not the media id: it contains the mark's end, which moves when the file duration is corrected.
      MediaItemData.Builder(item.chapter.id to item.markIndex)
        .setMediaItem(markMediaItems[index])
        .setDurationUs(item.mark.durationMs * 1000)
        .setIsSeekable(true)
        .setIsDynamic(false)
        .build()
    }
    return playlist
  }

  /**
   * The stored durations come from the scanner and can be off. [DurationInconsistenciesUpdater]
   * corrects them for the next time the book is loaded, but the marks of the current book have to
   * follow the duration the wrapped player resolved right away. Otherwise the last mark of a file
   * would end too early, cutting off its progress and its seek range.
   */
  private fun adoptResolvedDurations(timeline: Timeline) {
    val book = book ?: return
    if (timeline.windowCount != book.chapters.size) return
    val window = Timeline.Window()
    var changed = false
    val chapters = book.chapters.mapIndexed { index, chapter ->
      timeline.getWindow(index, window)
      val durationMs = window.durationMs
      if (window.isPlaceholder || durationMs == C.TIME_UNSET || durationMs == chapter.duration) {
        chapter
      } else {
        changed = true
        chapter.copy(duration = durationMs)
      }
    }
    if (!changed) return
    val playlist = applyBook(book.copy(chapters = chapters))
    reportedItemIndex = playlist.itemIndexFor(player.currentMediaItemIndex, player.contentPosition)
    registerBoundaryMessages(playlist)
    invalidateState()
  }

  fun setSkipSilenceEnabled(enabled: Boolean) {
    (player as? ExoPlayer)?.skipSilenceEnabled = enabled
  }

  /**
   * Crossing a mark boundary is a media item transition for everyone above us, but the wrapped
   * player just keeps playing the same file and reports nothing. These messages fire exactly at the
   * boundary so that the state is re-evaluated without polling. Whether a transition actually
   * happened is decided in [getState], because ExoPlayer also delivers a message when a seek lands
   * exactly on its position.
   */
  private fun registerBoundaryMessages(playlist: ChapterMarkPlaylist) {
    val exoPlayer = player as? ExoPlayer ?: return
    playlist.items.forEachIndexed { index, item ->
      if (item.mark.startMs <= 0) return@forEachIndexed
      val message = exoPlayer.createMessage { _, _ -> invalidateState() }
        .setPosition(playlist.fileIndexOf(index), item.mark.startMs)
        .setDeleteAfterDelivery(false)
        .setLooper(player.applicationLooper)
      boundaryMessages += message
      message.send()
    }
  }

  private fun clearBoundaryMessages() {
    boundaryMessages.forEach { it.cancel() }
    boundaryMessages.clear()
  }

  override fun getState(): State {
    val state = super.getState().withoutFileCommands()
    val book = book ?: return state
    val playlist = playlist ?: return state
    // The wrapped playlist is set asynchronously, so ignore states that do not match it yet.
    if (state.timeline.windowCount != book.chapters.size || markItems.isEmpty()) return state

    val fileIndex = state.currentMediaItemIndex.takeUnless { it == C.INDEX_UNSET } ?: 0
    val positionInFileSupplier = state.contentPositionMsSupplier
    val itemIndex = playlist.itemIndexFor(fileIndex, positionInFileSupplier.get()) ?: return state
    val mark = playlist.items[itemIndex].mark
    val bufferedSupplier = state.contentBufferedPositionMsSupplier
    val previousItemIndex = reportedItemIndex
    reportedItemIndex = itemIndex

    val builder = state.buildUpon()
      .setPlaylist(playlistWithCurrentItem(itemIndex, state))
      .setCurrentMediaItemIndex(itemIndex)
      .setAvailableCommands(availableCommands(state.availableCommands, itemIndex))
      .setContentPositionMs { mark.rebase(positionInFileSupplier.get()) }
      .setContentBufferedPositionMs { mark.rebase(bufferedSupplier.get()) }

    val crossedMark = previousItemIndex != null &&
      previousItemIndex != itemIndex &&
      !state.isSeekDiscontinuity()
    if (crossedMark) {
      builder.setPositionDiscontinuity(
        DISCONTINUITY_REASON_AUTO_TRANSITION,
        mark.rebase(positionInFileSupplier.get()),
      )
    } else if (state.hasPositionDiscontinuity) {
      builder.setPositionDiscontinuity(
        state.positionDiscontinuityReason,
        mark.rebase(state.discontinuityPositionMs),
      )
    }
    return builder.build()
  }

  /**
   * Repeat and shuffle would be applied to the files of the wrapped player, so they would act on
   * whole files instead of on the chapters we present.
   */
  private fun State.withoutFileCommands(): State {
    return buildUpon()
      .setAvailableCommands(
        availableCommands.buildUpon()
          .removeAll(COMMAND_SET_REPEAT_MODE, COMMAND_SET_SHUFFLE_MODE)
          .build(),
      )
      .build()
  }

  private fun State.isSeekDiscontinuity(): Boolean {
    return hasPositionDiscontinuity &&
      (
        positionDiscontinuityReason == DISCONTINUITY_REASON_SEEK ||
          positionDiscontinuityReason == DISCONTINUITY_REASON_SEEK_ADJUSTMENT
        )
  }

  private fun ChapterMark.rebase(positionInFileMs: Long): Long {
    return (positionInFileMs - startMs).coerceIn(0L, durationMs)
  }

  /**
   * The wrapped player combines the metadata embedded in the file with the file's media item.
   * Keep that and overlay the mark's own metadata, the same way ExoPlayer does for its items.
   */
  private fun playlistWithCurrentItem(
    itemIndex: Int,
    state: State,
  ): List<MediaItemData> {
    val current = markItems[itemIndex]
    val metadata = state.currentMetadata.buildUpon()
      .populate(current.mediaItem.mediaMetadata)
      .build()
    return markItems.toMutableList().also { items ->
      items[itemIndex] = current.buildUpon()
        .setTracks(state.currentTracks)
        .setMediaMetadata(metadata)
        .build()
    }
  }

  private fun availableCommands(
    commands: Player.Commands,
    itemIndex: Int,
  ): Player.Commands {
    val hasPrevious = itemIndex > 0
    val hasNext = itemIndex < markItems.lastIndex
    return commands.buildUpon()
      .addIf(COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM, hasPrevious)
      .addIf(COMMAND_SEEK_TO_PREVIOUS, hasPrevious)
      .addIf(COMMAND_SEEK_TO_NEXT_MEDIA_ITEM, hasNext)
      .addIf(COMMAND_SEEK_TO_NEXT, hasNext)
      .build()
  }

  override fun handleSeek(
    mediaItemIndex: Int,
    positionMs: Long,
    seekCommand: Int,
  ): ListenableFuture<*> {
    val playlist = playlist
    if (playlist == null || markItems.isEmpty()) {
      return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    }
    val itemIndex = mediaItemIndex.takeUnless { it == C.INDEX_UNSET } ?: currentMediaItemIndex
    val item = playlist.items.getOrNull(itemIndex)
    if (item == null) {
      Logger.w("handleSeek to unknown itemIndex=$itemIndex")
      return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    }
    val positionInItem = positionMs.takeUnless { it == C.TIME_UNSET } ?: 0L
    reportedItemIndex = itemIndex
    player.seekTo(
      playlist.fileIndexOf(itemIndex),
      item.mark.startMs + positionInItem.coerceIn(0L, item.mark.durationMs),
    )
    return Futures.immediateVoidFuture()
  }

  // Controllers can replace the whole book or clear it. Adding or editing single chapters would have to be
  // translated into edits of the files of the wrapped player, so such requests are ignored, also while no book is
  // loaded: items that come without their book have no file to play.

  override fun handleAddMediaItems(
    index: Int,
    mediaItems: List<MediaItem>,
  ): ListenableFuture<*> = ignoreChapterEdit("add")

  override fun handleMoveMediaItems(
    fromIndex: Int,
    toIndex: Int,
    newIndex: Int,
  ): ListenableFuture<*> = ignoreChapterEdit("move")

  override fun handleReplaceMediaItems(
    fromIndex: Int,
    toIndex: Int,
    mediaItems: List<MediaItem>,
  ): ListenableFuture<*> = ignoreChapterEdit("replace")

  override fun handleRemoveMediaItems(
    fromIndex: Int,
    toIndex: Int,
  ): ListenableFuture<*> {
    if (book == null) return super.handleRemoveMediaItems(fromIndex, toIndex)
    if (fromIndex > 0 || toIndex < markItems.size) return ignoreChapterEdit("remove")
    clear()
    return Futures.immediateVoidFuture()
  }

  private fun clear() {
    clearBoundaryMessages()
    book = null
    playlist = null
    markItems = emptyList()
    reportedItemIndex = null
    player.clearMediaItems()
  }

  private fun ignoreChapterEdit(operation: String): ListenableFuture<*> {
    Logger.w("Ignoring request to $operation chapters")
    return Futures.immediateVoidFuture()
  }

  override fun handleRelease(): ListenableFuture<*> {
    clearBoundaryMessages()
    return super.handleRelease()
  }
}
