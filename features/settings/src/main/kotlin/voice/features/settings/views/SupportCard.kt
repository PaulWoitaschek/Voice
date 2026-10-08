@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.settings.views

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.data.supporter.SupporterBadge
import voice.core.ui.BeatingHeart
import voice.core.ui.SupporterBadgeIcon
import voice.core.ui.label
import voice.core.ui.supporterSinceText
import java.time.YearMonth
import voice.core.strings.R as StringsR

@Composable
internal fun SupportCard(
  badge: SupporterBadge?,
  supporterSince: YearMonth?,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Surface(
    onClick = onClick,
    modifier = modifier.fillMaxWidth(),
    shape = IslandShape,
    color = MaterialTheme.colorScheme.tertiary,
    contentColor = MaterialTheme.colorScheme.onTertiary,
  ) {
    Row(
      modifier = Modifier.padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      if (badge != null) {
        SupporterBadgeIcon(
          badge = badge,
          size = 64.dp,
          contentDescription = badge.label(),
        )
      } else {
        BeatingHeart(
          containerColor = MaterialTheme.colorScheme.onTertiary,
          contentColor = MaterialTheme.colorScheme.tertiary,
        )
      }
      Column(Modifier.weight(1F)) {
        Text(
          text = stringResource(
            if (badge != null) {
              StringsR.string.settings_support_supporter_title
            } else {
              StringsR.string.settings_support_support_voice_title
            },
          ),
          style = MaterialTheme.typography.titleLargeEmphasized,
        )
        Text(
          text = if (badge != null && supporterSince != null) {
            supporterSinceText(supporterSince)
          } else {
            stringResource(StringsR.string.settings_support_support_voice_summary)
          },
          style = MaterialTheme.typography.bodyMedium,
          color = LocalContentColor.current.copy(alpha = 0.85F),
        )
      }
      Chevron()
    }
  }
}
