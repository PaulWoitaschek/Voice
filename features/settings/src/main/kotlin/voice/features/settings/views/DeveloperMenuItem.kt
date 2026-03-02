package voice.features.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Laptop
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
internal fun DeveloperMenuItem(onClick: () -> Unit) {
  ListItem(
    modifier = Modifier
      .fillMaxWidth()
      .clickable {
        onClick()
      },
    leadingContent = {
      Icon(
        imageVector = Icons.Outlined.Laptop,
        contentDescription = null,
      )
    },
    headlineContent = {
      Text(
        text = "Developer Menu",
      )
    },
  )
}
