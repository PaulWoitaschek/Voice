package voice.features.playbackScreen.view

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import kotlinx.coroutines.launch
import voice.core.data.BookId
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.rememberFullSizeCoverRequest
import voice.core.ui.sharedCoverElementModifier
import kotlin.math.abs
import kotlin.math.min
import kotlin.math.roundToInt
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

private const val SWIPE_THRESHOLD = 0.28F

/**
 * The cover breathes with playback: full size with a colored glow while playing, shrinking back
 * with softer corners when paused. Swipe it sideways to change chapters, double tap to play / pause.
 */
@Composable
internal fun PlaybackCover(
  bookId: BookId,
  cover: String?,
  playing: Boolean,
  swipeEnabled: Boolean,
  onPlayPause: () -> Unit,
  onSwipeToNext: () -> Unit,
  onSwipeToPrevious: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val scope = rememberCoroutineScope()
  val haptics = LocalHapticFeedback.current
  val scale by animateFloatAsState(
    targetValue = if (playing) 1F else 0.86F,
    animationSpec = spring(dampingRatio = 0.55F, stiffness = Spring.StiffnessLow),
    label = "coverScale",
  )
  val cornerPercent by animateFloatAsState(
    targetValue = if (playing) 8F else 14F,
    animationSpec = spring(dampingRatio = 0.7F, stiffness = Spring.StiffnessLow),
    label = "coverCorner",
  )
  val elevation by animateDpAsState(
    targetValue = if (playing) 28.dp else 4.dp,
    animationSpec = spring(stiffness = Spring.StiffnessVeryLow),
    label = "coverElevation",
  )
  // percentage corners scale with the cover, so they stay consistent during shared element transitions
  val shape = RoundedCornerShape(PercentCornerSize(cornerPercent))
  val glow = MaterialTheme.colorScheme.primary

  val offsetX = remember { Animatable(0F) }
  var width by remember { mutableIntStateOf(1) }
  var thresholdReached by remember { mutableStateOf(false) }
  val pop = remember { Animatable(1F) }
  var popShowsPlay by remember { mutableStateOf(false) }

  val currentPlaying by rememberUpdatedState(playing)
  val currentOnPlayPause by rememberUpdatedState(onPlayPause)
  val currentOnSwipeToNext by rememberUpdatedState(onSwipeToNext)
  val currentOnSwipeToPrevious by rememberUpdatedState(onSwipeToPrevious)

  Box(
    modifier = modifier
      .onSizeChanged { width = it.width.coerceAtLeast(1) }
      .graphicsLayer {
        val drag = offsetX.value / width
        translationX = offsetX.value
        rotationZ = drag * 12F
        alpha = 1F - abs(drag).coerceAtMost(1F) * 0.5F
      }
      .pointerInput(swipeEnabled) {
        if (!swipeEnabled) return@pointerInput
        detectHorizontalDragGestures(
          onDragStart = { thresholdReached = false },
          onDragEnd = {
            scope.launch {
              settleSwipe(
                offsetX = offsetX,
                width = width.toFloat(),
                onNext = { currentOnSwipeToNext() },
                onPrevious = { currentOnSwipeToPrevious() },
              )
            }
          },
          onDragCancel = {
            scope.launch { offsetX.animateTo(0F, spring(dampingRatio = 0.6F)) }
          },
          onHorizontalDrag = { change, dragAmount ->
            change.consume()
            val newOffset = offsetX.value + dragAmount
            scope.launch { offsetX.snapTo(newOffset) }
            val reached = abs(newOffset) > width * SWIPE_THRESHOLD
            if (reached != thresholdReached) {
              thresholdReached = reached
              if (reached) {
                haptics.performHapticFeedback(HapticFeedbackType.GestureThresholdActivate)
              }
            }
          },
        )
      }
      .pointerInput(Unit) {
        detectTapGestures(
          onDoubleTap = {
            popShowsPlay = !currentPlaying
            currentOnPlayPause()
            scope.launch {
              pop.snapTo(0F)
              pop.animateTo(1F, tween(durationMillis = 650))
            }
          },
        )
      }
      // Resting smaller while paused is a layout change rather than a draw transform, so shared
      // element transitions land directly on the final size instead of shrinking afterwards.
      .layout { measurable, constraints ->
        val coverSize = (min(constraints.maxWidth, constraints.maxHeight) * scale).roundToInt()
        val placeable = measurable.measure(Constraints.fixed(coverSize, coverSize))
        layout(constraints.maxWidth, constraints.maxHeight) {
          placeable.place((constraints.maxWidth - coverSize) / 2, (constraints.maxHeight - coverSize) / 2)
        }
      },
  ) {
    AsyncImage(
      modifier = Modifier
        .fillMaxSize()
        .sharedCoverElementModifier(bookId)
        // the glow is part of the shared content so it travels with the cover
        .shadow(elevation = elevation, shape = shape, ambientColor = glow, spotColor = glow)
        .clip(shape),
      contentScale = ContentScale.Crop,
      model = rememberFullSizeCoverRequest(cover),
      error = painterResource(id = UiR.drawable.album_art),
      contentDescription = stringResource(id = StringsR.string.cover_title),
    )
    if (pop.value < 1F) {
      Box(
        modifier = Modifier
          .align(Alignment.Center)
          .size(112.dp)
          .graphicsLayer {
            val p = pop.value
            alpha = if (p < 0.15F) p / 0.15F else 1F - (p - 0.15F) / 0.85F
            val popScale = 0.5F + 0.9F * (1F - (1F - p) * (1F - p))
            scaleX = popScale
            scaleY = popScale
          }
          .background(Color.Black.copy(alpha = 0.45F), CircleShape),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          modifier = Modifier.size(64.dp),
          imageVector = if (popShowsPlay) VoiceIcons.PlayArrow else VoiceIcons.Pause,
          contentDescription = null,
          tint = Color.White,
        )
      }
    }
  }
}

private suspend fun settleSwipe(
  offsetX: Animatable<Float, AnimationVector1D>,
  width: Float,
  onNext: () -> Unit,
  onPrevious: () -> Unit,
) {
  val offset = offsetX.value
  if (abs(offset) < width * SWIPE_THRESHOLD) {
    offsetX.animateTo(0F, spring(dampingRatio = 0.6F, stiffness = Spring.StiffnessMediumLow))
    return
  }
  val toNext = offset < 0
  val exit = if (toNext) -width * 1.3F else width * 1.3F
  offsetX.animateTo(exit, tween(durationMillis = 160, easing = FastOutLinearInEasing))
  if (toNext) onNext() else onPrevious()
  offsetX.snapTo(-exit)
  offsetX.animateTo(0F, spring(dampingRatio = 0.65F, stiffness = Spring.StiffnessLow))
}

/** Like `CornerSize(percent: Int)`, but fractional, so the corners animate smoothly. */
private data class PercentCornerSize(private val percent: Float) : CornerSize {
  override fun toPx(
    shapeSize: Size,
    density: Density,
  ): Float = shapeSize.minDimension * percent / 100F
}
