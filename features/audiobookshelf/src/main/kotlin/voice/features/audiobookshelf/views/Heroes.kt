@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.audiobookshelf.views

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import coil.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import voice.core.ui.ShapedIcon
import voice.core.ui.icons.VoiceIcons
import java.io.File
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin

/**
 * The server, turning slowly, with stories on their way to the listener circling around it.
 */
@Composable
internal fun ServerHero(
  clock: () -> Float,
  modifier: Modifier = Modifier,
) {
  val colors = MaterialTheme.colorScheme
  val bookColors = listOf(colors.tertiary, colors.secondary, colors.primary)
  BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
    val size = min(min(maxWidth, maxHeight) * 0.6F, 220.dp)
    Canvas(Modifier.size(size * 1.5F)) {
      val center = Offset(this.size.width / 2, this.size.height / 2)
      val radius = this.size.minDimension / 2 * 0.9F
      val seconds = clock()
      bookColors.forEachIndexed { index, color ->
        val angle = seconds * 0.35F + index * 2 * PI.toFloat() / bookColors.size
        val bob = sin(seconds * 1.3F + index) * radius * 0.04F
        val position = Offset(
          x = center.x + cos(angle) * (radius + bob),
          y = center.y + sin(angle) * (radius + bob) * 0.55F,
        )
        drawRoundRect(
          color = color,
          topLeft = position - Offset(radius * 0.09F, radius * 0.12F),
          size = Size(radius * 0.18F, radius * 0.24F),
          cornerRadius = CornerRadius(radius * 0.04F),
        )
      }
    }
    ShapedIcon(
      icon = VoiceIcons.Dns,
      shape = MaterialShapes.Cookie9Sided,
      containerColor = colors.primaryContainer,
      contentColor = colors.onPrimaryContainer,
      size = size * 0.75F,
      shapeRotation = clock() * 8F,
    )
  }
}

/**
 * A few books from the server, fanned out like a hand of cards.
 */
@Composable
internal fun CoversHero(
  covers: List<File>,
  modifier: Modifier = Modifier,
) {
  val shown = covers.take(5)
  val pops = remember(shown) { shown.map { Animatable(0F) } }
  LaunchedEffect(pops) {
    pops.forEachIndexed { index, pop ->
      launch {
        delay(120L * index)
        pop.animateTo(1F, spring(dampingRatio = 0.55F, stiffness = Spring.StiffnessLow))
      }
    }
  }
  BoxWithConstraints(modifier, contentAlignment = Alignment.Center) {
    val coverSize = min(min(maxWidth * 0.36F, maxHeight * 0.7F), 180.dp)
    val middle = (shown.size - 1) / 2F
    // the middle cover lies on top
    shown.indices.sortedByDescending { abs(it - middle) }.forEach { index ->
      val cover = shown[index]
      val offset = index - middle
      Box(
        Modifier
          .size(coverSize)
          .graphicsLayer {
            val pop = pops[index].value
            translationX = offset * coverSize.toPx() * 0.42F * pop
            translationY = abs(offset) * coverSize.toPx() * 0.06F
            rotationZ = offset * 7F * pop
            scaleX = 0.6F + 0.4F * pop
            scaleY = 0.6F + 0.4F * pop
            alpha = pop.coerceIn(0F, 1F)
          }
          .aspectRatio(1F)
          .shadow(8.dp, RoundedCornerShape(20.dp))
          .clip(RoundedCornerShape(20.dp)),
      ) {
        AsyncImage(
          model = cover,
          contentDescription = null,
          contentScale = ContentScale.Crop,
          modifier = Modifier.fillMaxSize(),
        )
      }
    }
  }
}
