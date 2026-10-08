@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.support

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import voice.core.data.supporter.SupporterBadge
import voice.core.ui.BeatingHeart
import voice.core.ui.SupporterBadgeIcon
import voice.core.ui.label
import voice.core.ui.supporterSinceText
import java.time.YearMonth
import voice.core.strings.R as StringsR

/**
 * The top of the support sheet: a beating heart asking for support, or the [badge] for someone who
 * already supports Voice.
 */
@Composable
internal fun SupportHero(
  badge: SupporterBadge?,
  supporterSince: YearMonth?,
  description: String,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(4.dp),
  ) {
    if (badge != null) {
      SupporterBadgeIcon(
        badge = badge,
        size = 88.dp,
        contentDescription = badge.label(),
      )
      Spacer(Modifier.height(12.dp))
      Text(
        text = stringResource(StringsR.string.support_supporter_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
      if (supporterSince != null) {
        Text(
          text = supporterSinceText(supporterSince),
          style = MaterialTheme.typography.labelLarge,
          color = MaterialTheme.colorScheme.primary,
          textAlign = TextAlign.Center,
        )
      }
    } else {
      BeatingHeart(
        containerColor = MaterialTheme.colorScheme.tertiaryContainer,
        contentColor = MaterialTheme.colorScheme.onTertiaryContainer,
        size = 88.dp,
      )
      Spacer(Modifier.height(12.dp))
      Text(
        text = stringResource(StringsR.string.support_hero_title),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
    }
    Spacer(Modifier.height(4.dp))
    Text(
      text = stringResource(StringsR.string.support_description_maintenance),
      style = MaterialTheme.typography.bodyLarge,
      textAlign = TextAlign.Center,
    )
    Text(
      text = description,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}
