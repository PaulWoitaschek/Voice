package voice.bookOverview.bottomSheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun BottomSheetContent(
  state: EditBookBottomSheetState,
  onItemClick: (BottomSheetItem) -> Unit,
) {
  Column {
    state.items.forEach { item ->
      ListItem(
        modifier = Modifier.clickable {
          onItemClick(item)
        },
        headlineContent = {
          Text(text = stringResource(item.titleRes))
        },
        leadingContent = {
          Icon(
            imageVector = item.icon,
            contentDescription = stringResource(item.titleRes),
          )
        },
      )
    }
    Spacer(modifier = Modifier.size(24.dp))
  }
}
