package voice.features.playbackScreen.view

import androidx.compose.foundation.gestures.AnchoredDraggableState
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigationevent.DirectNavigationEventInput
import androidx.navigationevent.NavigationEvent
import androidx.navigationevent.compose.LocalNavigationEventDispatcherOwner
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.ui.LocalPlayerDrag
import voice.core.ui.LocalSharedTransitionScope
import voice.core.ui.PlayerDrag

internal enum class DragPosition {
  Open,
  Closed,
}

// the drag acts as a back gesture, so the library shows behind the player while it slides down
@Stable
internal class DragDownToCloseState(private val playerDrag: PlayerDrag) {
  val input = DirectNavigationEventInput()
  val draggable = AnchoredDraggableState(DragPosition.Open)
  private var height = 0F

  var dragged: Boolean
    get() = playerDrag.active
    private set(value) {
      playerDrag.active = value
    }

  fun onHeightChanged(height: Int) {
    this.height = height.toFloat()
    draggable.updateAnchors(
      DraggableAnchors {
        DragPosition.Open at 0F
        DragPosition.Closed at height.toFloat()
      },
    )
  }

  suspend fun sendBackEvents(isSettled: () -> Boolean) {
    coroutineScope {
      launch {
        snapshotFlow { draggable.settledValue }.first { it == DragPosition.Closed }
        input.backCompleted()
      }
      snapshotFlow { progress() }
        .collect { progress ->
          if (!dragged && progress > 0F) {
            dragged = true
            input.backStarted(navigationEvent(0F))
          }
          if (dragged) {
            input.backProgressed(navigationEvent(progress))
            if (progress == 0F) {
              input.backCancelled()
              // the library stays for a moment, and the cover would fly to it
              snapshotFlow(isSettled).first { it }
              dragged = false
            }
          }
        }
    }
  }

  private fun progress(): Float {
    val offset = draggable.offset
    return if (offset.isNaN() || height == 0F) 0F else (offset / height).coerceIn(0F, 1F)
  }

  private fun navigationEvent(progress: Float) =
    NavigationEvent(progress = progress, swipeEdge = NavigationEvent.EDGE_NONE)
}

@Composable
internal fun rememberDragDownToCloseState(): DragDownToCloseState {
  val playerDrag = LocalPlayerDrag.current
  val state = remember(playerDrag) { DragDownToCloseState(playerDrag) }
  val dispatcher = LocalNavigationEventDispatcherOwner.current?.navigationEventDispatcher
  DisposableEffect(dispatcher, state) {
    dispatcher?.addInput(state.input)
    onDispose { dispatcher?.removeInput(state.input) }
  }
  // a closed player stays dragged, and the next one would open without its cover flying in
  DisposableEffect(playerDrag) { onDispose { playerDrag.active = false } }
  val scenes =
    LocalSharedTransitionScope.current?.let {
      LocalNavAnimatedContentScope.current.transition.parentTransition
    }
  LaunchedEffect(state, scenes) {
    state.sendBackEvents(
      isSettled = { scenes == null || scenes.currentState == scenes.targetState },
    )
  }
  return state
}

internal fun Modifier.dragDownToClose(state: DragDownToCloseState): Modifier = onSizeChanged {
  state.onHeightChanged(it.height)
}
  .anchoredDraggable(state.draggable, Orientation.Vertical)
// inside the draggable, so the drag isn't measured in the moving coordinates
.graphicsLayer {
    translationY = state.draggable.offset.takeUnless { it.isNaN() } ?: 0F
    // the aurora draws past the edges, over the library
    clip = true
  }
