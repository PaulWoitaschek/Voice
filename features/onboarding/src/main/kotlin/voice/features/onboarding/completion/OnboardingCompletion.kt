package voice.features.onboarding.completion

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.ui.OnboardingButton
import voice.core.ui.OnboardingScaffold
import voice.core.ui.OnboardingStep
import voice.core.ui.VoiceTheme
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface OnboardingCompletionGraph {
  val onboardingCompletionViewModel: OnboardingCompletionViewModel
}

@BindingContainer
@ContributesTo(AppScope::class)
object OnboardingCompletionProvider {

  @Provides
  @IntoSet
  fun onboardingCompletionNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.OnboardingCompletion> { key ->
    NavEntry(key) {
      OnboardingCompletion()
    }
  }
}

@Composable
fun OnboardingCompletion(modifier: Modifier = Modifier) {
  val viewModel = retain<OnboardingCompletionViewModel> {
    rootGraphAs<OnboardingCompletionGraph>().onboardingCompletionViewModel
  }
  OnboardingCompletion(
    modifier = modifier,
    onNext = viewModel::next,
    onBack = viewModel::back,
  )
}

@Composable
private fun OnboardingCompletion(
  onNext: () -> Unit,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
) {
  OnboardingScaffold(
    modifier = modifier,
    step = OnboardingStep.Completion,
    onBack = onBack,
    title = stringResource(StringsR.string.onboarding_completion_title),
    subtitle = stringResource(StringsR.string.onboarding_completion_subtitle),
    hero = { clock ->
      Celebration(clock = clock, modifier = Modifier.fillMaxSize())
    },
    actions = {
      OnboardingButton(
        text = stringResource(StringsR.string.onboarding_completion_action_start_listening),
        onClick = onNext,
        trailingArrow = true,
      )
    },
  )
}

@Composable
@Preview
private fun OnboardingCompletionPreview() {
  VoiceTheme {
    OnboardingCompletion({}, {})
  }
}
