package voice.features.onboarding.explanation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import voice.core.common.rootGraphAs
import voice.core.ui.OnboardingButton
import voice.core.ui.OnboardingScaffold
import voice.core.ui.OnboardingStep
import voice.core.ui.VoiceTheme
import voice.core.strings.R as StringsR

@Composable
fun OnboardingExplanation(
  // prepares for Android's folder picker in the guided variant of the onboarding experiment
  guided: Boolean,
  modifier: Modifier = Modifier,
) {
  val viewModel = retain<OnboardingExplanationViewModel> {
    rootGraphAs<OnboardingExplanationProvider>()
      .onboardingExplanationViewModel
  }
  OnboardingExplanation(
    guided = guided,
    modifier = modifier,
    onClose = viewModel::onClose,
    onNext = viewModel::onNext,
  )
}

@Composable
private fun OnboardingExplanation(
  guided: Boolean,
  onClose: () -> Unit,
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OnboardingScaffold(
    modifier = modifier,
    step = OnboardingStep.Explanation,
    onBack = onClose,
    title = stringResource(if (guided) StringsR.string.onboarding_guide_title else StringsR.string.onboarding_explanation_title),
    subtitle = stringResource(
      if (guided) StringsR.string.onboarding_guide_subtitle else StringsR.string.onboarding_explanation_subtitle_folder,
    ),
    hero = { clock ->
      if (guided) {
        FolderPickerGuide(clock = clock, modifier = Modifier.fillMaxSize())
      } else {
        Bookshelf(clock = clock, modifier = Modifier.fillMaxSize())
      }
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
private fun OnboardingExplanationPreview(
  @PreviewParameter(GuidedProvider::class)
  guided: Boolean,
) {
  VoiceTheme {
    OnboardingExplanation(
      guided = guided,
      onClose = {},
      onNext = {},
    )
  }
}

private class GuidedProvider : PreviewParameterProvider<Boolean> {
  override val values = sequenceOf(false, true)
}
