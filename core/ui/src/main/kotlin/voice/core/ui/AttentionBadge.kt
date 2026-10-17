package voice.core.ui

import androidx.compose.material3.Badge
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription

@Composable
fun AttentionBadge(
  contentDescription: String,
  modifier: Modifier = Modifier,
) {
  Badge(modifier.clearAndSetSemantics { this.contentDescription = contentDescription }) {
    Text("!")
  }
}
