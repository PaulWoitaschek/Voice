package voice.features.onboarding.explanation

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
fun OnboardingExplanation(modifier: Modifier = Modifier) {
  val viewModel = retain<OnboardingExplanationViewModel> {
    rootGraphAs<OnboardingExplanationProvider>()
      .onboardingExplanationViewModel
  }
  OnboardingExplanation(
    modifier = modifier,
    onClose = viewModel::onClose,
    onNext = viewModel::onNext,
  )
}

@Composable
private fun OnboardingExplanation(
  onClose: () -> Unit,
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OnboardingScaffold(
    modifier = modifier,
    step = OnboardingStep.Explanation,
    onBack = onClose,
    title = stringResource(StringsR.string.onboarding_explanation_title),
    subtitle = stringResource(StringsR.string.onboarding_explanation_subtitle_folder),
    hero = { clock ->
      Bookshelf(clock = clock, modifier = Modifier.fillMaxSize())
    },
    actions = {
      OnboardingButton(
        text = stringResource(StringsR.string.onboarding_action_next),
        onClick = onNext,
        trailingArrow = true,
      )
    },
  )
}

@Composable
@Preview
private fun OnboardingExplanationPreview() {
  VoiceTheme {
    OnboardingExplanation(
      onClose = {},
      onNext = {},
    )
  }
}
