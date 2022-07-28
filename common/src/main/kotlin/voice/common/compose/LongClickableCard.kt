package voice.common.compose

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.ripple.rememberRipple
import androidx.compose.material3.Card
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role

@Composable
fun LongClickableCard(
  onClick: () -> Unit,
  onLongClick: () -> Unit,
  modifier: Modifier = Modifier,
  content: @Composable () -> Unit
) {
  Card(modifier = modifier) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .combinedClickable(
          interactionSource = remember { MutableInteractionSource() },
          indication = rememberRipple(),
          enabled = true,
          role = Role.Button,
          onClick = onClick,
          onLongClick = onLongClick,
        )
    ) {
      content()
    }
  }
}
