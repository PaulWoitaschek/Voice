package voice.features.folderPicker.selectType

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import voice.core.strings.R as StringsR

@Composable
internal fun AddingFab(
  addButtonVisible: Boolean,
  onAddClick: () -> Unit,
) {
  var fabVisible by remember { mutableStateOf(false) }
  LaunchedEffect(addButtonVisible) {
    fabVisible = if (addButtonVisible) {
      delay(1000)
      true
    } else {
      false
    }
  }
  val density = LocalDensity.current
  AnimatedVisibility(
    visible = fabVisible,
    enter = slideInVertically {
      with(density) { 40.dp.roundToPx() }
    },
    exit = slideOutVertically(),
  ) {
    ExtendedFloatingActionButton(
      onClick = onAddClick,
      text = {
        Text(text = stringResource(id = StringsR.string.add))
      },
      icon = {
        Icon(
          imageVector = Icons.Outlined.Check,
          contentDescription = stringResource(id = StringsR.string.add),
        )
      },
    )
  }
}
