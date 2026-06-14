package voice.features.settings.views

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.selection.selectable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import voice.core.data.ThemeColorScheme
import voice.core.data.ThemeMode
import voice.core.strings.R as StringsR

@Composable
internal fun ThemeModeRow(
  themeMode: ThemeMode,
  onClick: () -> Unit,
) {
  SelectionRow(
    title = stringResource(StringsR.string.settings_appearance_theme_title),
    value = themeMode.label(),
    onClick = onClick,
  )
}

@Composable
internal fun ThemeColorSchemeRow(
  themeColorScheme: ThemeColorScheme,
  onClick: () -> Unit,
) {
  SelectionRow(
    title = stringResource(StringsR.string.settings_appearance_color_scheme_title),
    value = themeColorScheme.label(),
    onClick = onClick,
  )
}

@Composable
internal fun ThemeModeDialog(
  selectedThemeMode: ThemeMode,
  onThemeModeSelect: (ThemeMode) -> Unit,
  onDismiss: () -> Unit,
) {
  var temporarySelection by remember(selectedThemeMode) {
    mutableStateOf(selectedThemeMode)
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(StringsR.string.settings_appearance_theme_title))
    },
    text = {
      Column {
        ThemeMode.entries.forEach { themeMode ->
          SelectionDialogItem(
            text = themeMode.label(),
            selected = themeMode == temporarySelection,
            onClick = {
              temporarySelection = themeMode
            },
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onThemeModeSelect(temporarySelection)
        },
        content = {
          Text(stringResource(StringsR.string.common_dialog_confirm))
        },
      )
    },
  )
}

@Composable
internal fun ThemeColorSchemeDialog(
  selectedThemeColorScheme: ThemeColorScheme,
  onThemeColorSchemeSelect: (ThemeColorScheme) -> Unit,
  onDismiss: () -> Unit,
) {
  var temporarySelection by remember(selectedThemeColorScheme) {
    mutableStateOf(selectedThemeColorScheme)
  }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = {
      Text(stringResource(StringsR.string.settings_appearance_color_scheme_title))
    },
    text = {
      Column {
        ThemeColorScheme.entries.forEach { themeColorScheme ->
          SelectionDialogItem(
            text = themeColorScheme.label(),
            supportingText = themeColorScheme.supportingText(),
            selected = themeColorScheme == temporarySelection,
            onClick = {
              temporarySelection = themeColorScheme
            },
          )
        }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          onThemeColorSchemeSelect(temporarySelection)
        },
        content = {
          Text(stringResource(StringsR.string.common_dialog_confirm))
        },
      )
    },
  )
}

@Composable
private fun SelectionRow(
  title: String,
  value: String,
  onClick: () -> Unit,
) {
  ListItem(
    modifier = Modifier
      .clickable {
        onClick()
      }
      .fillMaxWidth(),
    headlineContent = {
      Text(text = title)
    },
    supportingContent = {
      Text(text = value)
    },
  )
}

@Composable
private fun SelectionDialogItem(
  text: String,
  selected: Boolean,
  supportingText: String? = null,
  onClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .selectable(
        selected = selected,
        onClick = onClick,
        role = Role.RadioButton,
      )
      .padding(vertical = 16.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    RadioButton(
      selected = selected,
      onClick = null,
    )
    Spacer(Modifier.width(16.dp))
    Column {
      Text(
        text = text,
        style = MaterialTheme.typography.bodyLarge,
      )
      if (supportingText != null) {
        Text(
          text = supportingText,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
  }
}

@Composable
private fun ThemeMode.label(): String {
  return when (this) {
    ThemeMode.FollowSystem -> stringResource(StringsR.string.settings_appearance_theme_follow_system)
    ThemeMode.Light -> stringResource(StringsR.string.settings_appearance_theme_light)
    ThemeMode.Dark -> stringResource(StringsR.string.settings_appearance_theme_dark)
  }
}

@Composable
private fun ThemeColorScheme.label(): String {
  return when (this) {
    ThemeColorScheme.VoiceBlue -> stringResource(StringsR.string.settings_appearance_color_scheme_voice_blue)
    ThemeColorScheme.Dynamic -> stringResource(StringsR.string.settings_appearance_color_scheme_dynamic)
  }
}

@Composable
private fun ThemeColorScheme.supportingText(): String? {
  return when (this) {
    ThemeColorScheme.VoiceBlue -> null
    ThemeColorScheme.Dynamic -> stringResource(StringsR.string.settings_appearance_color_scheme_dynamic_summary)
  }
}
