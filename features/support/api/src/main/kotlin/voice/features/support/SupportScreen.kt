@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.support

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.ui.VoiceTheme
import voice.navigation.BottomSheetNav
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR

@Composable
@Preview
private fun SupportPlayPreview() {
  VoiceTheme {
    Support(
      viewState = SupportViewState.preview(),
      listener = SupportListener.noop(),
    )
  }
}

@Composable
@Preview
private fun SupportKoFiPreview() {
  VoiceTheme {
    Support(
      viewState = SupportViewState.preview(content = SupportViewState.Content.KoFi).copy(badge = null, supporterSince = null),
      listener = SupportListener.noop(),
    )
  }
}

@Composable
@Preview
private fun SupportThankYouPreview() {
  VoiceTheme {
    Support(
      viewState = SupportViewState.preview().copy(thankYou = true),
      listener = SupportListener.noop(),
    )
  }
}

@Composable
private fun Support(
  viewState: SupportViewState,
  listener: SupportListener,
) {
  AnimatedContent(
    targetState = viewState.thankYou,
    transitionSpec = { fadeIn() togetherWith fadeOut() },
    label = "thankYou",
  ) { thankYou ->
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .padding(horizontal = 24.dp)
        .padding(bottom = 32.dp),
      verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
      if (thankYou) {
        ThankYou(
          badge = viewState.badge,
          onDone = listener::close,
        )
      } else {
        SupportHero(
          badge = viewState.badge,
          supporterSince = viewState.supporterSince,
          description = stringResource(
            if (viewState.content == SupportViewState.Content.KoFi) {
              StringsR.string.support_description_maintenance_subtitle
            } else {
              StringsR.string.support_description_play
            },
          ),
        )
        when (val content = viewState.content) {
          SupportViewState.Content.KoFi -> KoFiContent(onKoFiClick = listener::openKoFi)
          SupportViewState.Content.Loading -> {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
              LoadingIndicator()
            }
          }
          SupportViewState.Content.Unavailable -> Unavailable(onRetry = listener::retry)
          is SupportViewState.Content.Play -> PlayContent(
            content = content,
            message = viewState.message,
            badge = viewState.badge,
            listener = listener,
          )
        }
      }
    }
  }
}

@Composable
private fun Unavailable(onRetry: () -> Unit) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Text(
      text = stringResource(StringsR.string.support_unavailable_title),
      style = MaterialTheme.typography.titleMedium,
      textAlign = TextAlign.Center,
    )
    Text(
      text = stringResource(StringsR.string.support_unavailable_message),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
    FilledTonalButton(onClick = onRetry) {
      Text(stringResource(StringsR.string.common_error_generic_retry))
    }
  }
}

@ContributesTo(AppScope::class)
interface SupportGraph {
  val supportViewModel: SupportViewModel
}

@BindingContainer
@ContributesTo(AppScope::class)
object SupportProvider {

  @Provides
  @IntoSet
  fun supportNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.SupportVoice> { key ->
    NavEntry(
      key,
      metadata = BottomSheetNav.bottomSheet(),
    ) {
      Support()
    }
  }
}

@Composable
fun Support() {
  val viewModel = retain<SupportViewModel> { rootGraphAs<SupportGraph>().supportViewModel }
  Support(viewModel.viewState(), viewModel)
}
