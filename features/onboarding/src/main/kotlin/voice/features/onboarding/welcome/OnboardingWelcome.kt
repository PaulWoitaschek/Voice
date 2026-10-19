package voice.features.onboarding.welcome

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import voice.core.common.rootGraphAs
import voice.core.ui.OnboardingButton
import voice.core.ui.OnboardingScaffold
import voice.core.ui.OnboardingStep
import voice.core.ui.VoiceTheme
import voice.core.strings.R as StringsR

@Composable
fun OnboardingWelcome(modifier: Modifier = Modifier) {
  val viewModel = retain<OnboardingWelcomeViewModel> {
    rootGraphAs<OnboardingWelcomeProvider>()
      .onboardingWelcomeViewModel
  }
  OnboardingWelcome(
    modifier = modifier,
    viewState = viewModel.viewState(),
    onContinueWithAnalytics = viewModel::onContinueWithAnalytics,
    onContinueWithoutAnalytics = viewModel::onContinueWithoutAnalytics,
    onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick,
  )
}

@Composable
private fun OnboardingWelcome(
  viewState: OnboardingWelcomeViewState,
  onContinueWithAnalytics: () -> Unit,
  onContinueWithoutAnalytics: () -> Unit,
  onPrivacyPolicyClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OnboardingScaffold(
    modifier = modifier,
    step = OnboardingStep.Welcome,
    onBack = null,
    title = stringResource(StringsR.string.onboarding_welcome_title),
    subtitle = stringResource(StringsR.string.onboarding_welcome_subtitle),
    hero = { clock ->
      VoiceIcon(clock = clock, modifier = Modifier.fillMaxSize())
    },
    details = {
      if (viewState.askForAnalytics) {
        Spacer(Modifier.height(24.dp))
        Text(
          text = stringResource(StringsR.string.onboarding_analytics_consent_description),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          textAlign = TextAlign.Center,
        )
        TextButton(onClick = onPrivacyPolicyClick) {
          Text(stringResource(StringsR.string.onboarding_analytics_consent_action_privacy_policy))
        }
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
          text = stringResource(StringsR.string.onboarding_welcome_action_start),
          onClick = onContinueWithoutAnalytics,
          trailingArrow = true,
        )
      }
    },
  )
}

private class OnboardingWelcomePreviewParameterProvider : PreviewParameterProvider<OnboardingWelcomeViewState> {

  override val values: Sequence<OnboardingWelcomeViewState>
    get() = sequenceOf(
      OnboardingWelcomeViewState(askForAnalytics = true),
      OnboardingWelcomeViewState(askForAnalytics = false),
    )
}

@Composable
@Preview
private fun OnboardingWelcomePreview(
  @PreviewParameter(OnboardingWelcomePreviewParameterProvider::class)
  viewState: OnboardingWelcomeViewState,
) {
  VoiceTheme {
    OnboardingWelcome(
      viewState = viewState,
      onContinueWithAnalytics = {},
      onContinueWithoutAnalytics = {},
      onPrivacyPolicyClick = {},
    )
  }
}
