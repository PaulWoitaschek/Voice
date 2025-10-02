package voice.features.onboarding.completion

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.ui.VoiceTheme
import voice.core.ui.rememberScoped
import voice.features.onboarding.R
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface OnboardingCompletionGraph {
  val onboardingCompletionViewModel: OnboardingCompletionViewModel
}

@ContributesTo(AppScope::class)
interface OnboardingCompletionProvider {

  @Provides
  @IntoSet
  fun navEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.OnboardingCompletion> { key ->
    NavEntry(key) {
      OnboardingCompletion()
    }
  }
}

@Composable
fun OnboardingCompletion(modifier: Modifier = Modifier) {
  val viewModel = rememberScoped {
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
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { },
        navigationIcon = {
          IconButton(onClick = onBack) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(id = StringsR.string.close),
            )
          }
        },
      )
    },
    floatingActionButton = {
      ExtendedFloatingActionButton(onClick = onNext) {
        Text(text = stringResource(StringsR.string.onboarding_completion_next_button))
      }
    },
    content = { contentPadding ->
      Column(Modifier.padding(contentPadding)) {
        if (shouldShowImage()) {
          Image(
            modifier = Modifier
              .weight(1F)
              .heightIn(max = 400.dp)
              .padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
              .align(Alignment.CenterHorizontally),
            painter = painterResource(id = R.drawable.completion_artwork),
            contentDescription = null,
          )
        }

        Column(Modifier.weight(2F)) {
          Spacer(modifier = Modifier.size(16.dp))
          Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(StringsR.string.onboarding_completion_title),
            style = MaterialTheme.typography.displayMedium,
          )
          Spacer(modifier = Modifier.size(4.dp))
          Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(StringsR.string.onboarding_completion_subtitle),
            style = MaterialTheme.typography.bodyLarge,
          )
        }
      }
    },
  )
}

@Composable
private fun shouldShowImage(): Boolean {
  val localWindowInfo = LocalWindowInfo.current
  val thresholdPx = with(LocalDensity.current) { 500.dp.toPx() }
  return localWindowInfo.containerSize.height > thresholdPx
}

@Composable
@Preview
private fun OnboardingCompletionPreview() {
  VoiceTheme {
    OnboardingCompletion({}, {})
  }
}
