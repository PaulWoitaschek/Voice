package voice.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.common.compose.VoiceTheme
import voice.strings.R as StringsR

@Composable
fun OnboardingWelcome(
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
    content = { contentPadding ->
      Column(Modifier.padding(contentPadding)) {
        if (LocalConfiguration.current.screenHeightDp > 440) {
          Spacer(modifier = Modifier.size(64.dp))
          VoiceIcon(
            modifier = Modifier
              .padding(horizontal = 32.dp)
              .align(Alignment.CenterHorizontally),
          )
        }
        Spacer(modifier = Modifier.size(32.dp))
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
    },
  )
}

@Composable
private fun VoiceIcon(
  modifier: Modifier = Modifier,
) {
  Image(
    modifier = modifier
      .size(256.dp)
      .clip(CircleShape),
    painter = painterResource(id = R.drawable.welcome),
    contentDescription = null,
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
