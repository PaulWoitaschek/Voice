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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.common.compose.VoiceTheme

@Composable
fun OnboardingWelcome(
  onNext: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    floatingActionButton = {
      ExtendedFloatingActionButton(onClick = onNext) {
        Text(text = "Next")
      }
    },
    content = { contentPadding ->
      Column(Modifier.padding(contentPadding)) {
        Spacer(modifier = Modifier.size(64.dp))
        VoiceIcon(modifier = Modifier.align(Alignment.CenterHorizontally))
        Spacer(modifier = Modifier.size(32.dp))
        Text(
          modifier = Modifier.padding(horizontal = 24.dp),
          text = "Welcome to Voice!",
          style = MaterialTheme.typography.displayMedium,
        )
        Spacer(modifier = Modifier.size(4.dp))
        Text(
          modifier = Modifier.padding(horizontal = 24.dp),
          text = "Your personal pocket audiobook player. Let's get you set up.",
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
