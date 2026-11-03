package voice.core.playback.session

import android.content.Context
import android.os.Bundle
import androidx.annotation.StringRes
import androidx.media3.common.PlaybackParameters
import androidx.media3.common.Player
import androidx.media3.session.CommandButton
import androidx.media3.session.SessionCommand
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlin.math.abs
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.core.data.repo.BookRepository
import voice.core.data.repo.BookmarkRepo
import voice.core.playback.di.PlaybackScope
import voice.core.playback.impl.R
import voice.core.playback.player.VoicePlayer
import voice.core.playback.playstate.PositionUpdater
import voice.core.strings.R as StringsR

@Inject
@SingleIn(PlaybackScope::class)
class MediaButtons(
  private val context: Context,
  private val player: VoicePlayer,
  private val androidAutoConnection: AndroidAutoConnection,
  private val bookRepository: BookRepository,
  private val bookmarkRepo: BookmarkRepo,
  private val positionUpdater: PositionUpdater,
  private val scope: CoroutineScope,
) {

  private val rewind =
    CommandButton.Builder(CommandButton.ICON_SKIP_BACK)
      .setDisplayName(context.getString(StringsR.string.playback_action_rewind))
      .setPlayerCommand(Player.COMMAND_SEEK_BACK)
      .setSlots(CommandButton.SLOT_BACK)
      .build()

  private val fastForward =
    CommandButton.Builder(CommandButton.ICON_SKIP_FORWARD)
      .setDisplayName(context.getString(StringsR.string.playback_action_fast_forward))
      .setPlayerCommand(Player.COMMAND_SEEK_FORWARD)
      .setSlots(CommandButton.SLOT_FORWARD)
      .build()

  val phone: List<CommandButton> = listOf(rewind, fastForward)

  // Android Auto shows the buttons of the notification, so they change while the phone is connected
  // to a car.
  fun preferences(): Flow<List<CommandButton>> =
    androidAutoConnection.connected.flatMapLatest { connected ->
      if (connected) {
        player.playbackSpeed().map(::car)
      } else {
        flowOf(phone)
      }
    }

  private fun car(speed: Float): List<CommandButton> =
    listOf(
      rewind,
      fastForward,
      overflowButton(CommandButton.ICON_PREVIOUS, StringsR.string.playback_chapter_previous)
        .setPlayerCommand(Player.COMMAND_SEEK_TO_PREVIOUS)
        .build(),
      overflowButton(CommandButton.ICON_NEXT, StringsR.string.playback_chapter_next)
        .setPlayerCommand(Player.COMMAND_SEEK_TO_NEXT)
        .build(),
      // Media3 has no icon for adding a bookmark
      overflowButton(CommandButton.ICON_UNDEFINED, StringsR.string.bookmark_save_moment)
        .setCustomIconResId(R.drawable.ic_bookmark_add)
        .setSessionCommand(ADD_BOOKMARK)
        .build(),
      overflowButton(speedIcon(speed), StringsR.string.playback_speed_title)
        .setSessionCommand(NEXT_SPEED)
        .build(),
    )

  private fun overflowButton(
    icon: Int,
    @StringRes name: Int,
  ): CommandButton.Builder =
    CommandButton.Builder(icon)
      .setDisplayName(context.getString(name))
      .setSlots(CommandButton.SLOT_OVERFLOW)

  fun onCustomCommand(command: SessionCommand): Boolean {
    when (command.customAction) {
      ADD_BOOKMARK.customAction -> scope.launch { addBookmark() }
      NEXT_SPEED.customAction ->
        player.setPlaybackSpeed(nextCarSpeed(player.playbackParameters.speed))
      else -> return false
    }
    return true
  }

  private suspend fun addBookmark() {
    positionUpdater.flushPositionNow()
    val bookId = player.currentMediaItem?.mediaId?.toMediaIdOrNull()?.bookId ?: return
    val book = bookRepository.get(bookId) ?: return
    bookmarkRepo.addBookmarkAtBookPosition(book, title = null)
  }

  companion object {
    internal val ADD_BOOKMARK = SessionCommand("voice.addBookmark", Bundle.EMPTY)
    internal val NEXT_SPEED = SessionCommand("voice.nextSpeed", Bundle.EMPTY)
    internal val commands = listOf(ADD_BOOKMARK, NEXT_SPEED)
  }
}

// Media3 only has icons for these speeds.
private val carSpeeds =
  mapOf(
    1F to CommandButton.ICON_PLAYBACK_SPEED_1_0,
    1.2F to CommandButton.ICON_PLAYBACK_SPEED_1_2,
    1.5F to CommandButton.ICON_PLAYBACK_SPEED_1_5,
    1.8F to CommandButton.ICON_PLAYBACK_SPEED_1_8,
    2F to CommandButton.ICON_PLAYBACK_SPEED_2_0,
  )

private const val SPEED_TOLERANCE = 0.01F

internal fun nextCarSpeed(speed: Float): Float {
  return carSpeeds.keys.firstOrNull { it > speed + SPEED_TOLERANCE } ?: carSpeeds.keys.first()
}

internal fun speedIcon(speed: Float): Int {
  return carSpeeds.entries.firstOrNull { abs(it.key - speed) < SPEED_TOLERANCE }?.value
    ?: CommandButton.ICON_PLAYBACK_SPEED
}

private fun Player.playbackSpeed(): Flow<Float> = callbackFlow {
  val listener =
    object : Player.Listener {
      override fun onPlaybackParametersChanged(playbackParameters: PlaybackParameters) {
        trySend(playbackParameters.speed)
      }
    }
  send(playbackParameters.speed)
  addListener(listener)
  awaitClose { removeListener(listener) }
}
  .distinctUntilChanged()
