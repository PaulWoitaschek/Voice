@file:OptIn(ExperimentalMaterial3Api::class)

package voice.features.cover

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.material3.SearchBarValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberSearchBarState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun CoverSearchBar(
  query: TextFieldState,
  onSearch: () -> Unit,
  onCloseClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
  SearchBarDefaults.InputField(
    textFieldState = query,
    // this screen is all search, so the field never collapses
    searchBarState = rememberSearchBarState(initialValue = SearchBarValue.Expanded),
    onSearch = { onSearch() },
    modifier = modifier.fillMaxWidth(),
    placeholder = {
      Text(stringResource(StringsR.string.cover_search_hint))
    },
    leadingIcon = {
      IconButton(onClick = onCloseClick) {
        Icon(
          imageVector = VoiceIcons.ArrowBack,
          contentDescription = stringResource(StringsR.string.common_action_back),
        )
      }
    },
    trailingIcon = if (query.text.isNotEmpty()) {
      {
        IconButton(onClick = { query.clearText() }) {
          Icon(
            imageVector = VoiceIcons.Close,
            contentDescription = stringResource(StringsR.string.library_search_clear),
          )
        }
      }
    } else {
      null
    },
    shape = SearchBarDefaults.inputFieldShape,
    colors = SearchBarDefaults.inputFieldColors(
      focusedContainerColor = containerColor,
      unfocusedContainerColor = containerColor,
    ),
  )
}
