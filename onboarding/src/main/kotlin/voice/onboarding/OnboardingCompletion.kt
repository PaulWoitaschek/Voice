package voice.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowBack
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import voice.common.compose.VoiceTheme
import voice.strings.R as StringsR

@Composable
fun OnboardingCompletion(
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
              imageVector = Icons.Outlined.ArrowBack,
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
        Spacer(modifier = Modifier.size(24.dp))
        Image(
          modifier = Modifier
            .widthIn(max = 400.dp)
            .padding(horizontal = 32.dp)
            .align(Alignment.CenterHorizontally),
          painter = painterResource(id = R.drawable.completion_artwork),
          contentDescription = null,
        )

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
