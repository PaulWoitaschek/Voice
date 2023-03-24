package voice.bookOverview.search

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.strings.R as StringsR

@Composable
internal fun SearchTextField(
  query: String,
  onQueryChange: (String) -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  TextField(
    modifier = Modifier
      .focusRequester(focusRequester)
      .padding(start = 54.dp, end = 8.dp)
      .fillMaxWidth(),
    colors = TextFieldDefaults.textFieldColors(
      containerColor = Color.Transparent,
      focusedIndicatorColor = Color.Transparent,
      unfocusedIndicatorColor = Color.Transparent,
    ),
    value = query,
    singleLine = true,
    maxLines = 1,
    onValueChange = onQueryChange,
    label = {
      Text(stringResource(id = StringsR.string.search_hint))
    },
  )
  SideEffect {
    focusRequester.requestFocus()
  }
}
