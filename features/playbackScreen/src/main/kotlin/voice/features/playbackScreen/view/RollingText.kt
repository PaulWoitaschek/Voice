package voice.features.playbackScreen.view

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.LayoutDirection

/**
 * Text where every character rolls like an odometer when it changes.
 * Characters are keyed from the end so that "9:59" -> "10:00" keeps the trailing digits in place.
 *
 * Only meant for numbers, times and the like, which read left to right in every locale. Words
 * must not be split into characters, as many scripts join or reorder them.
 */
@Composable
internal fun RollingText(
  text: String,
  style: TextStyle,
  modifier: Modifier = Modifier,
  color: Color = Color.Unspecified,
) {
  val tabularStyle = remember(style) { style.copy(fontFeatureSettings = "tnum") }
  CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Ltr) {
    Row(modifier = modifier.clearAndSetSemantics { contentDescription = text }) {
      text.forEachIndexed { index, char ->
        key(text.length - index) {
          AnimatedContent(
            targetState = char,
            transitionSpec = {
              val up = rollsUp(from = initialState, to = targetState)
              val enter = slideInVertically { height -> if (up) height else -height } + fadeIn()
              val exit = slideOutVertically { height -> if (up) -height else height } + fadeOut()
              (enter togetherWith exit).using(SizeTransform(clip = true))
            },
            label = "rollingChar",
          ) { targetChar ->
            Text(text = targetChar.toString(), style = tabularStyle, color = color)
          }
        }
      }
    }
  }
}

private fun rollsUp(
  from: Char,
  to: Char,
): Boolean {
  if (!from.isDigit() || !to.isDigit()) return true
  return when {
    from == '9' && to == '0' -> true
    from == '0' && to == '9' -> false
    else -> to > from
  }
}
