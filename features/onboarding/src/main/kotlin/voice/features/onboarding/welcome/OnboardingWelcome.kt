package voice.features.onboarding.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.core.common.rootGraphAs
import voice.core.ui.VoiceTheme
import voice.core.ui.rememberScoped
import voice.features.onboarding.R
import voice.core.strings.R as StringsR

@Composable
fun OnboardingWelcome(modifier: Modifier = Modifier) {
  val viewModel = rememberScoped {
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
  Scaffold(
    modifier = modifier,
    floatingActionButton = {
      ExtendedFloatingActionButton(onClick = onNext) {
        Text(text = stringResource(StringsR.string.onboarding_button_next))
      }
    },
    topBar = {
      TopAppBar(title = { })
    },
    content = { contentPadding ->
      Column(Modifier.padding(contentPadding)) {
        if (shouldShowImage()) {
          Image(
            modifier = Modifier
              .weight(1F)
              .padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
              .heightIn(max = 400.dp)
              .aspectRatio(1F)
              .fillMaxSize()
              .align(Alignment.CenterHorizontally)
              .clip(CircleShape),
            painter = painterResource(id = R.drawable.welcome),
            contentDescription = null,
          )
        }
        Column(Modifier.weight(2F)) {
          Spacer(modifier = Modifier.size(16.dp))
          Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(StringsR.string.onboarding_welcome_title),
            style = MaterialTheme.typography.displayMedium,
          )
          Spacer(modifier = Modifier.size(4.dp))
          Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(id = StringsR.string.onboarding_welcome_subtitle),
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
  val thresholdPx = with(LocalDensity.current) { 440.dp.toPx() }
  return localWindowInfo.containerSize.height > thresholdPx
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
