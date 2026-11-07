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
import voice.core.data.toUri
import voice.core.logging.api.Logger
import voice.core.playback.di.PlaybackScope
import voice.core.playback.session.ChapterMarkPlaylist
import voice.core.playback.session.MediaId
import voice.core.playback.session.durationMs
import voice.core.playback.session.encode
import voice.core.playback.session.toMediaIdOrNull

/**
 * Presents the wrapped player, which holds one media item per audio file, as a playlist with one
 * media item per chapter mark.
 *
 * A single file book is therefore extracted once instead of once per chapter, while the session,
 * the notification and the UI still see chapters as individual playlist entries.
 */
@Inject
@SingleIn(PlaybackScope::class)
class ChapterMarkPlayer(private val player: ExoPlayer) : ForwardingSimpleBasePlayer(player) {

  private var playlist: ChapterMarkPlaylist? = null
  private var markItems: List<MediaItemData> = emptyList()
  private var reportedItemIndex: Int? = null
  private val boundaryMessages = mutableListOf<PlayerMessage>()

  var onChapterEnding: () -> Unit = {}

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

  override fun handleSetMediaItems(
    mediaItems: List<MediaItem>,
    startIndex: Int,
    startPositionMs: Long,
  ): ListenableFuture<*> {
    val playlist = applyChapterMarks(mediaItems)
    if (playlist == null) {
      Logger.w("Ignoring ${mediaItems.size} media items that are not all chapter marks")
      return Futures.immediateVoidFuture()
    }
    val itemIndex = startIndex.takeIf { it in playlist.marks.indices } ?: 0
    val mark = playlist.marks[itemIndex]
    val positionInItemMs = startPositionMs.takeUnless { it == C.TIME_UNSET } ?: 0L
    reportedItemIndex = itemIndex
    player.setMediaItems(
      playlist.fileMediaItems(),
      playlist.fileIndexOf(itemIndex),
      mark.startMs + positionInItemMs.coerceIn(0L, mark.durationMs),
    )
    registerBoundaryMessages(playlist)
    return Futures.immediateVoidFuture()
  }

  private fun applyChapterMarks(mediaItems: List<MediaItem>): ChapterMarkPlaylist? {
    val marks = mediaItems.map {
      it.mediaId.toMediaIdOrNull() as? MediaId.ChapterMark ?: return null
    }
    if (marks.isEmpty()) return null
    clearBoundaryMessages()
    val playlist = ChapterMarkPlaylist(marks)
    this.playlist = playlist
    markItems = mediaItems.mapIndexed { index, mediaItem ->
      val mark = marks[index]
      // Not the media id: it contains the mark's end, which moves when the file duration is
      // corrected.
      MediaItemData.Builder(mark.chapterId to mark.markIndex)
        .setMediaItem(mediaItem)
        .setDurationUs(mark.durationMs * 1000)
        .setIsSeekable(true)
        .setIsDynamic(false)
        .build()
    }
    return playlist
  }

  private fun ChapterMarkPlaylist.fileMediaItems(): List<MediaItem> {
    val bookId = marks.first().bookId
    return files.map { file ->
      MediaItem.Builder()
        .setMediaId(MediaId.Chapter(bookId = bookId, chapterId = file).encode())
        .setUri(file.toUri())
        .build()
    }
  }

  /**
   * The stored durations come from the scanner and can be off. [DurationInconsistenciesUpdater]
   * corrects them for the next time the book is loaded, but the marks of the current book have to
   * follow the duration the wrapped player resolved right away. Otherwise the last mark of a file
   * would end too early, cutting off its progress and its seek range.
   */
  private fun adoptResolvedDurations(timeline: Timeline) {
    val playlist = playlist ?: return
    if (timeline.windowCount != playlist.files.size) return
    val window = Timeline.Window()
    val mediaItems = markItems.map { it.mediaItem }.toMutableList()
    var changed = false
    playlist.files.indices.forEach { fileIndex ->
      timeline.getWindow(fileIndex, window)
      val durationMs = window.durationMs
      if (window.isPlaceholder || durationMs == C.TIME_UNSET) return@forEach
      val itemIndex = playlist.lastItemIndexOf(fileIndex)
      val mark = playlist.marks[itemIndex]
      // the last mark ends 1ms before the end of the file, as in Chapter.chapterMarks
      val endMs = (durationMs - 1).coerceAtLeast(mark.startMs + 1)
      if (endMs != mark.endMs) {
        changed = true
        mediaItems[itemIndex] = mediaItems[itemIndex].withMark(mark.copy(endMs = endMs))
      }
    }
    if (!changed) return
    val updated = applyChapterMarks(mediaItems) ?: return
    reportedItemIndex = updated.itemIndexFor(player.currentMediaItemIndex, player.contentPosition)
    registerBoundaryMessages(updated)
    invalidateState()
  }

  private fun MediaItem.withMark(mark: MediaId.ChapterMark): MediaItem {
    return buildUpon()
      .setMediaId(mark.encode())
      .setMediaMetadata(mediaMetadata.buildUpon().setDurationMs(mark.durationMs).build())
      .build()
  }

  fun setSkipSilenceEnabled(enabled: Boolean) {
    player.skipSilenceEnabled = enabled
  }

  /**
   * Crossing a mark boundary is a media item transition for everyone above us, but the wrapped
   * player just keeps playing the same file and reports nothing. These messages fire exactly at the
   * boundary so that the state is re-evaluated without polling. Whether a transition actually
   * happened is decided in [getState], because ExoPlayer also delivers a message when a seek lands
   * exactly on its position.
   */
  private fun registerBoundaryMessages(playlist: ChapterMarkPlaylist) {
    playlist.marks.forEachIndexed { index, mark ->
      if (mark.startMs > 0) {
        sendMessage(playlist, index, mark.startMs) { invalidateState() }
      }
      val endingMs = (mark.endMs - CHAPTER_ENDING_MS).coerceAtLeast(mark.startMs)
      sendMessage(playlist, index, endingMs) { onChapterEnding() }
    }
  }

  private fun sendMessage(
    playlist: ChapterMarkPlaylist,
    itemIndex: Int,
    positionInFileMs: Long,
    action: () -> Unit,
  ) {
    val message =
      player
        .createMessage { _, _ -> action() }
        .setPosition(playlist.fileIndexOf(itemIndex), positionInFileMs)
        .setDeleteAfterDelivery(false)
        .setLooper(player.applicationLooper)
    boundaryMessages += message
    message.send()
  }

  private fun clearBoundaryMessages() {
    boundaryMessages.forEach { it.cancel() }
    boundaryMessages.clear()
  }

  override fun getState(): State {
    val state = super.getState().withPresentedCommands()
    val playlist = playlist ?: return state
    // The wrapped playlist is set asynchronously, so ignore states that do not match it yet.
    if (state.timeline.windowCount != playlist.files.size) return state

    val fileIndex = state.currentMediaItemIndex.takeUnless { it == C.INDEX_UNSET } ?: 0
    val positionInFileSupplier = state.contentPositionMsSupplier
    val itemIndex = playlist.itemIndexFor(fileIndex, positionInFileSupplier.get()) ?: return state
    val mark = playlist.marks[itemIndex]
    val bufferedSupplier = state.contentBufferedPositionMsSupplier
    val previousItemIndex = reportedItemIndex
    reportedItemIndex = itemIndex

    val builder =
      state
        .buildUpon()
        .setPlaylist(playlistWithCurrentItem(itemIndex, state))
        .setCurrentMediaItemIndex(itemIndex)
        .setContentPositionMs { mark.rebase(positionInFileSupplier.get()) }
        .setContentBufferedPositionMs { mark.rebase(bufferedSupplier.get()) }

    val crossedMark =
      previousItemIndex != null && previousItemIndex != itemIndex && !state.isSeekDiscontinuity()
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
   *
   * Next and previous stay available in the first and the last chapter, because [VoicePlayer] turns
   * them into skips, and headsets and cars only send them while they are available.
   */
  private fun State.withPresentedCommands(): State {
    val commands =
      availableCommands.buildUpon().removeAll(COMMAND_SET_REPEAT_MODE, COMMAND_SET_SHUFFLE_MODE)
    if (!timeline.isEmpty) {
      commands.addAll(
        COMMAND_SEEK_TO_PREVIOUS_MEDIA_ITEM,
        COMMAND_SEEK_TO_PREVIOUS,
        COMMAND_SEEK_TO_NEXT_MEDIA_ITEM,
        COMMAND_SEEK_TO_NEXT,
      )
    }
    return buildUpon().setAvailableCommands(commands.build()).build()
  }

  private fun State.isSeekDiscontinuity(): Boolean {
    return hasPositionDiscontinuity &&
      (positionDiscontinuityReason == DISCONTINUITY_REASON_SEEK ||
        positionDiscontinuityReason == DISCONTINUITY_REASON_SEEK_ADJUSTMENT)
  }

  private fun MediaId.ChapterMark.rebase(positionInFileMs: Long): Long {
    return (positionInFileMs - startMs).coerceIn(0L, durationMs)
  }

  /**
   * The wrapped player combines the metadata embedded in the file with the file's media item. Keep
   * that and overlay the mark's own metadata, the same way ExoPlayer does for its items.
   */
  private fun playlistWithCurrentItem(
    itemIndex: Int,
    state: State,
  ): List<MediaItemData> {
    val current = markItems[itemIndex]
    val metadata =
      state.currentMetadata.buildUpon().populate(current.mediaItem.mediaMetadata).build()
    return markItems.toMutableList().also { items ->
      items[itemIndex] =
        current.buildUpon().setTracks(state.currentTracks).setMediaMetadata(metadata).build()
    }
  }

  override fun handleSeek(
    mediaItemIndex: Int,
    positionMs: Long,
    seekCommand: Int,
  ): ListenableFuture<*> {
    val playlist = playlist ?: return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    // a seek without a target, like next in the last chapter, does nothing
    if (mediaItemIndex == C.INDEX_UNSET) return Futures.immediateVoidFuture()
    val itemIndex = mediaItemIndex
    val mark = playlist.marks.getOrNull(itemIndex)
    if (mark == null) {
      Logger.w("handleSeek to unknown itemIndex=$itemIndex")
      return super.handleSeek(mediaItemIndex, positionMs, seekCommand)
    }
    val positionInItem = positionMs.takeUnless { it == C.TIME_UNSET } ?: 0L
    reportedItemIndex = itemIndex
    player.seekTo(
      playlist.fileIndexOf(itemIndex),
      mark.startMs + positionInItem.coerceIn(0L, mark.durationMs),
    )
    return Futures.immediateVoidFuture()
  }

  // Controllers can replace the whole book or clear it.
  // Editing single chapters would have to be translated into edits of the files of the wrapped
  // player, so such requests are ignored.

  override fun handleAddMediaItems(
    index: Int,
    mediaItems: List<MediaItem>,
  ): ListenableFuture<*> {
    if (playlist == null) return super.handleAddMediaItems(index, mediaItems)
    return ignoreChapterEdit("add")
  }

  override fun handleMoveMediaItems(
    fromIndex: Int,
    toIndex: Int,
    newIndex: Int,
  ): ListenableFuture<*> {
    if (playlist == null) return super.handleMoveMediaItems(fromIndex, toIndex, newIndex)
    return ignoreChapterEdit("move")
  }

  override fun handleReplaceMediaItems(
    fromIndex: Int,
    toIndex: Int,
    mediaItems: List<MediaItem>,
  ): ListenableFuture<*> {
    if (playlist == null) return super.handleReplaceMediaItems(fromIndex, toIndex, mediaItems)
    return ignoreChapterEdit("replace")
  }

  override fun handleRemoveMediaItems(
    fromIndex: Int,
    toIndex: Int,
  ): ListenableFuture<*> {
    if (playlist == null) return super.handleRemoveMediaItems(fromIndex, toIndex)
    if (fromIndex > 0 || toIndex < markItems.size) return ignoreChapterEdit("remove")
    clearBoundaryMessages()
    playlist = null
    markItems = emptyList()
    reportedItemIndex = null
    player.clearMediaItems()
    return Futures.immediateVoidFuture()
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

// More than the audio ExoPlayer processes ahead of what plays, even at the highest speed.
private const val CHAPTER_ENDING_MS = 5_000L
