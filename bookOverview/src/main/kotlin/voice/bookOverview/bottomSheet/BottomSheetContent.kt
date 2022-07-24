package voice.bookOverview.bottomSheet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp

@Composable
internal fun BottomSheetContent(
  state: EditBookBottomSheetState,
  onItemClicked: (BottomSheetItem) -> Unit
) {
  Column(Modifier.padding(vertical = 8.dp)) {
    state.items.forEach { item ->
      Row(
        modifier = Modifier
          .heightIn(min = 48.dp)
          .clickable {
            onItemClicked(item)
          }
          .padding(horizontal = 16.dp)
          .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          imageVector = item.icon,
          contentDescription = stringResource(item.titleRes),
          modifier = Modifier.size(24.dp)
        )
        Spacer(modifier = Modifier.size(32.dp))
        Text(text = stringResource(item.titleRes))
      }
    }
  }
}
