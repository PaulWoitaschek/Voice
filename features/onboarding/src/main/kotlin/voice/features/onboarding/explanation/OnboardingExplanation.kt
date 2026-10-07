@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.onboarding.explanation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
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
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import voice.core.common.rootGraphAs
import voice.core.ui.OnboardingButton
import voice.core.ui.OnboardingScaffold
import voice.core.ui.OnboardingStep
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.strings.R as StringsR

@Composable
fun OnboardingExplanation(modifier: Modifier = Modifier) {
  val viewModel = retain<OnboardingExplanationViewModel> {
    rootGraphAs<OnboardingExplanationProvider>()
      .onboardingExplanationViewModel
  }
  OnboardingExplanation(
    modifier = modifier,
    viewState = viewModel.viewState(),
    onClose = viewModel::onClose,
    onContinueWithAnalytics = viewModel::onContinueWithAnalytics,
    onContinueWithoutAnalytics = viewModel::onContinueWithoutAnalytics,
    onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick,
  )
}

@Composable
fun OnboardingExplanation(
  viewState: OnboardingExplanationViewState,
  onClose: () -> Unit,
  onContinueWithAnalytics: () -> Unit,
  onContinueWithoutAnalytics: () -> Unit,
  onPrivacyPolicyClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OnboardingScaffold(
    modifier = modifier,
    step = OnboardingStep.Explanation,
    onBack = onClose,
    title = stringResource(StringsR.string.onboarding_explanation_title),
    subtitle = stringResource(StringsR.string.onboarding_explanation_subtitle),
    hero = { clock ->
      Bookshelf(clock = clock, modifier = Modifier.fillMaxSize())
    },
    details = {
      if (viewState.askForAnalytics) {
        Spacer(Modifier.height(24.dp))
        AnalyticsConsentCard(onPrivacyPolicyClick = onPrivacyPolicyClick)
      }
    },
    actions = {
      if (viewState.askForAnalytics) {
        OnboardingButton(
          text = stringResource(StringsR.string.onboarding_analytics_consent_action_disable),
          onClick = onContinueWithoutAnalytics,
          primary = false,
        )
        OnboardingButton(
          text = stringResource(StringsR.string.onboarding_analytics_consent_action_enable),
          onClick = onContinueWithAnalytics,
        )
      } else {
        OnboardingButton(
          text = stringResource(StringsR.string.onboarding_action_next),
          onClick = onContinueWithoutAnalytics,
          trailingArrow = true,
        )
      }
    },
  )
}

@Composable
private fun AnalyticsConsentCard(onPrivacyPolicyClick: () -> Unit) {
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

private class OnboardingExplanationPreviewParameterProvider : PreviewParameterProvider<OnboardingExplanationViewState> {

  override val values: Sequence<OnboardingExplanationViewState>
    get() = sequenceOf(
      OnboardingExplanationViewState(askForAnalytics = true),
      OnboardingExplanationViewState(askForAnalytics = false),
    )
}

@Composable
@Preview
private fun OnboardingExplanationPreview(
  @PreviewParameter(OnboardingExplanationPreviewParameterProvider::class)
  viewState: OnboardingExplanationViewState,
) {
  VoiceTheme {
    OnboardingExplanation(
      viewState = viewState,
      onClose = {},
      onContinueWithAnalytics = {},
      onContinueWithoutAnalytics = {},
      onPrivacyPolicyClick = {},
    )
  }
}
