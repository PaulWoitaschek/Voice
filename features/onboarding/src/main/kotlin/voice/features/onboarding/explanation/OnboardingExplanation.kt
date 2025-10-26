package voice.features.onboarding.explanation

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FabPosition
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.tooling.preview.PreviewParameterProvider
import androidx.compose.ui.unit.dp
import voice.core.common.rootGraphAs
import voice.core.ui.VoiceTheme
import voice.core.ui.rememberScoped
import voice.features.onboarding.R
import voice.core.strings.R as StringsR

@Composable
fun OnboardingExplanation(modifier: Modifier = Modifier) {
  val viewModel = rememberScoped {
    rootGraphAs<OnboardingExplanationProvider>()
      .onboardingExplanationViewModel
  }
  OnboardingExplanation(
    modifier = modifier,
    viewState = viewModel.viewState(),
    onClose = viewModel::onClose,
    onContinueWithAnalytics = viewModel::onContinueWithAnalytics,
    onContinueWithoutAnalytics = viewModel::onContinueWithoutAnalytics,
    onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick,
  )
}

@Composable
fun OnboardingExplanation(
  viewState: OnboardingExplanationViewState,
  onClose: () -> Unit,
  onContinueWithAnalytics: () -> Unit,
  onContinueWithoutAnalytics: () -> Unit,
  onPrivacyPolicyClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  Scaffold(
    modifier = modifier,
    topBar = {
      TopAppBar(
        title = { },
        navigationIcon = {
          IconButton(onClick = onClose) {
            Icon(
              imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
              contentDescription = stringResource(id = StringsR.string.close),
            )
          }
        },
      )
    },
    floatingActionButtonPosition = if (viewState.askForAnalytics) FabPosition.Center else FabPosition.End,
    floatingActionButton = {
      if (viewState.askForAnalytics) {
        Column(
          modifier = Modifier
            .sizeIn(maxWidth = 320.dp)
            .padding(horizontal = 24.dp, vertical = 16.dp),
          horizontalAlignment = CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          OutlinedButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueWithoutAnalytics,
          ) {
            Text(stringResource(StringsR.string.onboarding_analytics_consent_button_disable))
          }

          ExtendedFloatingActionButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onContinueWithAnalytics,
          ) {
            Text(stringResource(StringsR.string.onboarding_analytics_consent_button_enable))
          }

          Text(
            text = stringResource(StringsR.string.onboarding_analytics_consent_privacy_policy),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
              .padding(top = 4.dp)
              .align(CenterHorizontally)
              .clickable { onPrivacyPolicyClick() },
          )
        }
      } else {
        ExtendedFloatingActionButton(onClick = onContinueWithoutAnalytics) {
          Text(stringResource(StringsR.string.onboarding_button_next))
        }
      }
    },
    content = { contentPadding ->
      Column(Modifier.padding(contentPadding)) {
        if (shouldShowImage()) {
          Image(
            modifier = Modifier
              .padding(top = 32.dp, bottom = 24.dp, start = 24.dp, end = 24.dp)
              .weight(1F)
              .heightIn(max = 400.dp)
              .padding(horizontal = 32.dp)
              .align(CenterHorizontally),
            painter = painterResource(id = R.drawable.bookshelf_artwork),
            contentDescription = null,
          )
        }
        Column(Modifier.weight(2F)) {
          Spacer(modifier = Modifier.size(16.dp))
          Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(StringsR.string.onboarding_explanation_title),
            style = MaterialTheme.typography.displayMedium,
          )
          Spacer(modifier = Modifier.size(4.dp))
          Text(
            modifier = Modifier.padding(horizontal = 24.dp),
            text = stringResource(StringsR.string.onboarding_explanation_subtitle),
            style = MaterialTheme.typography.bodyLarge,
          )

          if (viewState.askForAnalytics) {
            Spacer(modifier = Modifier.size(32.dp))

            Text(
              modifier = Modifier.padding(horizontal = 24.dp),
              text = stringResource(StringsR.string.onboarding_analytics_consent_title),
              style = MaterialTheme.typography.titleMedium,
            )
            Spacer(modifier = Modifier.size(8.dp))
            Text(
              modifier = Modifier.padding(horizontal = 24.dp),
              text = stringResource(StringsR.string.onboarding_analytics_consent_description),
              style = MaterialTheme.typography.bodyMedium,
            )
          }
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

private class OnboardingExplanationPreviewParameterProvider : PreviewParameterProvider<OnboardingExplanationViewState> {

  override val values: Sequence<OnboardingExplanationViewState>
    get() = sequenceOf(
      OnboardingExplanationViewState(askForAnalytics = true),
      OnboardingExplanationViewState(askForAnalytics = false),
    )
}

@Composable
@Preview
private fun OnboardingExplanationPreview(
  @PreviewParameter(OnboardingExplanationPreviewParameterProvider::class)
  viewState: OnboardingExplanationViewState,
) {
  VoiceTheme {
    OnboardingExplanation(
      viewState = viewState,
      onClose = {},
      onContinueWithAnalytics = {},
      onContinueWithoutAnalytics = {},
      onPrivacyPolicyClick = {},
    )
  }
}
