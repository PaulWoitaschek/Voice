@file:OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterial3ExpressiveApi::class)

package voice.features.bookOverview.select

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FloatingToolbarDefaults
import androidx.compose.material3.HorizontalFloatingToolbar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.data.BookId
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR
import voice.core.ui.VoiceTheme

@Composable
internal fun SelectionTopBar(
  selection: SelectionViewState,
  onClose: () -> Unit,
  onSelectAll: () -> Unit,
  modifier: Modifier = Modifier,
) {
  TopAppBar(
    modifier = modifier,
    navigationIcon = {
      IconButton(onClick = onClose) {
        Icon(
          painter = painterResource(UiR.drawable.ic_close),
          contentDescription = stringResource(StringsR.string.common_action_close),
        )
      }
    },
    title = {
      val count = selection.ids.size
      Text(pluralStringResource(StringsR.plurals.library_selection_count, count, count))
    },
    actions = {
      if (!selection.allSelected) {
        TextButton(onClick = onSelectAll) {
          Text(stringResource(StringsR.string.library_selection_select_all))
        }
      }
    },
  )
}

@Composable
internal fun SelectionToolbar(
  allHidden: Boolean,
  onEdit: (BookField) -> Unit,
  onHide: () -> Unit,
  onShow: () -> Unit,
  modifier: Modifier = Modifier,
) {
  HorizontalFloatingToolbar(
    expanded = true,
    modifier = modifier,
    colors = FloatingToolbarDefaults.vibrantFloatingToolbarColors(),
  ) {
    // the same icons as the grouping in the library filter
    ToolbarButton(
      icon = UiR.drawable.ic_collections_bookmark,
      label = StringsR.string.book_edit_series_label,
      onClick = { onEdit(BookField.Series) },
    )
    ToolbarButton(
      icon = UiR.drawable.ic_person,
      label = StringsR.string.book_edit_author_label,
      onClick = { onEdit(BookField.Author) },
    )
    if (allHidden) {
      ToolbarButton(
        icon = UiR.drawable.ic_visibility,
        label = StringsR.string.library_selection_show,
        onClick = onShow,
      )
    } else {
      ToolbarButton(
        icon = UiR.drawable.ic_visibility_off,
        label = StringsR.string.library_selection_hide,
        onClick = onHide,
      )
    }
  }
}

@Composable
private fun ToolbarButton(
  @DrawableRes icon: Int,
  @StringRes label: Int,
  onClick: () -> Unit,
) {
  // with large fonts, three labels don't fit next to each other
  if (LocalDensity.current.fontScale >= 1.5F) {
    IconButton(onClick = onClick) {
      Icon(painter = painterResource(icon), contentDescription = stringResource(label))
    }
  } else {
    TextButton(
      onClick = onClick,
      colors = ButtonDefaults.textButtonColors(contentColor = LocalContentColor.current),
      shapes = ButtonDefaults.shapes(),
    ) {
      Icon(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.IconSize),
      )
      Spacer(Modifier.width(ButtonDefaults.IconSpacing))
      Text(stringResource(label))
    }
  }
}

@Composable
@Preview
private fun SelectionBarsPreview() {
  VoiceTheme {
    Surface(color = MaterialTheme.colorScheme.surface) {
      Column {
        SelectionTopBar(
          selection =
            SelectionViewState(
              ids = setOf(BookId("1"), BookId("2"), BookId("3"), BookId("4")),
              allSelected = false,
              allHidden = false,
            ),
          onClose = {},
          onSelectAll = {},
        )
        SelectionToolbar(
          allHidden = false,
          onEdit = {},
          onHide = {},
          onShow = {},
          modifier = Modifier.padding(16.dp),
        )
      }
    }
  }
}
