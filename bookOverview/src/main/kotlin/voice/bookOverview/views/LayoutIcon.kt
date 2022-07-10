package voice.bookOverview.views

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.ViewList
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import voice.bookOverview.R
import voice.bookOverview.overview.BookOverviewViewState

@Composable
internal fun LayoutIcon(layoutIcon: BookOverviewViewState.Content.LayoutIcon, onClick: () -> Unit) {
  IconButton(onClick) {
    Icon(
      imageVector = when (layoutIcon) {
        BookOverviewViewState.Content.LayoutIcon.List -> Icons.Outlined.ViewList
        BookOverviewViewState.Content.LayoutIcon.Grid -> Icons.Outlined.GridView
      },
      contentDescription = stringResource(
        when (layoutIcon) {
          BookOverviewViewState.Content.LayoutIcon.List -> R.string.layout_list
          BookOverviewViewState.Content.LayoutIcon.Grid -> R.string.layout_grid
        }
      )
    )
  }
}
