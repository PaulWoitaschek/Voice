package voice.features.onboarding.welcome

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
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
  OnboardingWelcome(modifier = modifier, onNext = viewModel::next)
}

@Composable
private fun OnboardingWelcome(
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OnboardingScaffold(
    modifier = modifier,
    step = OnboardingStep.Welcome,
    onBack = null,
    title = stringResource(StringsR.string.onboarding_welcome_title),
    subtitle = stringResource(StringsR.string.onboarding_welcome_subtitle),
    hero = { clock ->
      SoundBlob(clock = clock, modifier = Modifier.fillMaxSize())
    },
    actions = {
      OnboardingButton(
        text = stringResource(StringsR.string.onboarding_welcome_action_start),
        onClick = onNext,
        trailingArrow = true,
      )
    },
  )
}

@Composable
@Preview
private fun OnboardingWelcomePreview() {
  VoiceTheme {
    OnboardingWelcome(
      onNext = {},
    )
  }
}
