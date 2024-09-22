package voice.cover

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SearchBar
import androidx.compose.material3.SearchBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.strings.R

@Composable
internal fun CoverSearchBar(
  onCloseClick: () -> Unit,
  onQueryChange: (String) -> Unit,
  viewState: SelectCoverFromInternetViewModel.ViewState,
) {
  SearchBar(
    inputField = {
      SearchBarDefaults.InputField(
        query = viewState.query,
        onQueryChange = onQueryChange,
        onSearch = {},
        expanded = false,
        onExpandedChange = {},
        enabled = true,
        placeholder = null,
        leadingIcon = {
          IconButton(onClick = onCloseClick) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(id = R.string.close),
            )
          }
        },
        trailingIcon = {
          IconButton(
            onClick = {
              onQueryChange("")
            },
          ) {
            Icon(
              imageVector = Icons.Outlined.Close,
              contentDescription = stringResource(id = R.string.delete),
            )
          }
        },
        interactionSource = null,
      )
    },
    expanded = false,
    onExpandedChange = {},
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    shape = SearchBarDefaults.inputFieldShape,
    tonalElevation = SearchBarDefaults.TonalElevation,
    shadowElevation = SearchBarDefaults.ShadowElevation,
    windowInsets = SearchBarDefaults.windowInsets,
    content = { },
  )
}
