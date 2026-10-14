package voice.core.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.contentColorFor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons

@Composable
fun CheckBadge(
  visible: Boolean,
  modifier: Modifier = Modifier,
  containerColor: Color = MaterialTheme.colorScheme.primary,
  outlineColor: Color = Color.Transparent,
) {
  AnimatedVisibility(
    modifier = modifier,
    visible = visible,
    enter = scaleIn(spring(dampingRatio = 0.4F, stiffness = Spring.StiffnessMedium)) + fadeIn(),
    exit = scaleOut() + fadeOut(),
  ) {
    Box(
      modifier = Modifier
        .size(24.dp)
        .background(containerColor, CircleShape)
        .border(2.dp, outlineColor, CircleShape),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = VoiceIcons.Check,
        contentDescription = null,
        tint = contentColorFor(containerColor),
        modifier = Modifier.size(16.dp),
      )
    }
  }
}
