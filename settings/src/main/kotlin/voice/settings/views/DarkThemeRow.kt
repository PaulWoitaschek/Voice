package voice.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.ListItem
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import voice.settings.R

@Composable
internal fun DarkThemeRow(useDarkTheme: Boolean, toggle: () -> Unit) {
  ListItem(
    modifier = Modifier
      .clickable {
        toggle()
      }
      .fillMaxWidth(),
    text = {
      Text(text = stringResource(R.string.pref_theme_dark))
    },
    trailing = {
      Switch(
        checked = useDarkTheme,
        onCheckedChange = {
          toggle()
        },
      )
    },
  )
}
