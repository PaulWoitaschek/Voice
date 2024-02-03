package voice.onboarding.completion

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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.squareup.anvil.annotations.ContributesTo
import voice.common.AppScope
import voice.common.compose.VoiceTheme
import voice.common.compose.rememberScoped
import voice.common.rootComponentAs
import voice.onboarding.R
import voice.strings.R as StringsR

@ContributesTo(AppScope::class)
interface OnboardingCompletionComponent {
  val viewModel: OnboardingCompletionViewModel
}

@Composable
fun OnboardingCompletion(modifier: Modifier = Modifier) {
  val viewModel = rememberScoped {
    rootComponentAs<OnboardingCompletionComponent>().viewModel
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
        if (LocalConfiguration.current.screenHeightDp > 500) {
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
@Preview
private fun OnboardingCompletionPreview() {
  VoiceTheme {
    OnboardingCompletion({}, {})
  }
}
