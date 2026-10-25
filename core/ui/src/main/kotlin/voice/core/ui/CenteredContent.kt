package voice.core.ui

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max

val MaxContentWidth = 640.dp

@Composable
fun CenteredContent(
  modifier: Modifier = Modifier,
  content: @Composable (horizontalPadding: Dp) -> Unit,
) {
  BoxWithConstraints(modifier) {
    content(max(16.dp, (maxWidth - MaxContentWidth) / 2))
  }
}
