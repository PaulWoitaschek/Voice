@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.onboarding.welcome

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.toShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
internal fun AnalyticsConsentCard(onPrivacyPolicyClick: () -> Unit) {
  Surface(
    modifier = Modifier.fillMaxWidth(),
    shape = RoundedCornerShape(28.dp),
    color = MaterialTheme.colorScheme.surfaceContainerHigh,
  ) {
    Row(Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 4.dp)) {
      Box(
        modifier = Modifier
          .size(44.dp)
          .background(MaterialTheme.colorScheme.secondaryContainer, MaterialShapes.Clover4Leaf.toShape()),
        contentAlignment = Alignment.Center,
      ) {
        Icon(
          imageVector = VoiceIcons.Analytics,
          contentDescription = null,
          tint = MaterialTheme.colorScheme.onSecondaryContainer,
          modifier = Modifier.size(22.dp),
        )
      }
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = stringResource(StringsR.string.onboarding_analytics_consent_title),
          style = MaterialTheme.typography.titleMediumEmphasized,
        )
        Spacer(Modifier.height(4.dp))
        Text(
          text = stringResource(StringsR.string.onboarding_analytics_consent_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        TextButton(
          onClick = onPrivacyPolicyClick,
          modifier = Modifier.padding(top = 4.dp),
        ) {
          Text(stringResource(StringsR.string.onboarding_analytics_consent_action_privacy_policy))
        }
      }
    }
  }
}
