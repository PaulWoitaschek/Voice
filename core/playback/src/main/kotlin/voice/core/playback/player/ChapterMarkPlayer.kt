package voice.core.playback.player

import androidx.media3.common.C
import androidx.media3.common.ForwardingSimpleBasePlayer
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.PlayerMessage
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import voice.core.data.ChapterMark
import voice.core.data.durationMs
import voice.core.logging.api.Logger
import voice.core.playback.di.PlaybackScope
import voice.core.playback.session.ChapterMarkPlaylist

/**
 * Presents the wrapped player, which holds one media item per audio file, as a playlist with one
 * media item per chapter mark.
 *
 * A single file book is therefore extracted once instead of once per chapter, while the session,
 * the notification and the UI still see chapters as individual playlist entries.
 */
@Inject
@SingleIn(PlaybackScope::class)
class ChapterMarkPlayer(private val player: Player) : ForwardingSimpleBasePlayer(player) {

  private var playlist: ChapterMarkPlaylist? = null
  private var markItems: List<MediaItemData> = emptyList()
  private var fileItemCount: Int = 0
  private var reportedItemIndex: Int? = null
  private val boundaryMessages = mutableListOf<PlayerMessage>()

  internal fun setBook(
    playlist: ChapterMarkPlaylist,
    markMediaItems: List<MediaItem>,
    fileMediaItems: List<MediaItem>,
    startItemIndex: Int,
    positionInItemMs: Long,
  ) {
    clearBoundaryMessages()
    this.playlist = playlist
    this.fileItemCount = fileMediaItems.size
    markItems = playlist.items.mapIndexed { index, item ->
      MediaItemData.Builder(item.mediaId)
        .setMediaItem(markMediaItems[index])
        .setDurationUs(item.mark.durationMs * 1000)
        .setIsSeekable(true)
        .setIsDynamic(false)
        .build()
    }
    reportedItemIndex = startItemIndex
    val startItem = playlist.items[startItemIndex]
    player.setMediaItems(
      fileMediaItems,
      playlist.fileIndexOf(startItemIndex),
      startItem.mark.startMs + positionInItemMs,
    )
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
    val state = super.getState()
    val playlist = playlist ?: return state
    // The wrapped playlist is set asynchronously, so ignore states that do not match it yet.
    if (state.timeline.windowCount != fileItemCount || markItems.isEmpty()) return state

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

  override fun handleRelease(): ListenableFuture<*> {
    clearBoundaryMessages()
    return super.handleRelease()
  }
}
