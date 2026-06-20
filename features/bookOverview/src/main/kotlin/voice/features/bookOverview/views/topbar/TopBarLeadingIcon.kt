package voice.features.bookOverview.views.topbar

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.strings.R
import voice.core.ui.icons.ArrowBack
import voice.core.ui.icons.Search
import voice.core.ui.icons.VoiceIcons

@Composable
internal fun ColumnScope.TopBarLeadingIcon(
  searchActive: Boolean,
  onActiveChange: (Boolean) -> Unit,
) {
  AnimatedVisibility(
    visible = searchActive,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    IconButton(onClick = { onActiveChange(false) }) {
      Icon(
        imageVector = VoiceIcons.ArrowBack,
        contentDescription = stringResource(id = R.string.common_action_close),
      )
    }
  }
  AnimatedVisibility(
    visible = !searchActive,
    enter = fadeIn(),
    exit = fadeOut(),
  ) {
    Box(Modifier.size(48.dp), contentAlignment = Alignment.Center) {
      Icon(
        imageVector = VoiceIcons.Search,
        contentDescription = stringResource(id = R.string.library_search_hint),
      )
    }
  }
}
