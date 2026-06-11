package voice.features.support

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Coffee
import androidx.compose.material.icons.outlined.Construction
import androidx.compose.material.icons.outlined.LockOpen
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.retain.retain
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
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
import voice.navigation.BottomSheetNav
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@Composable
@Preview
private fun SupportPreview() {
  VoiceTheme {
    Support(
      viewState = SupportViewState.preview(),
      listener = SupportListener.noop(),
    )
  }
}

@Composable
private fun Support(
  viewState: SupportViewState,
  listener: SupportListener,
) {
  Column(
    modifier = Modifier
      .fillMaxWidth()
      .verticalScroll(rememberScrollState())
      .padding(horizontal = 24.dp)
      .padding(bottom = 48.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    when (viewState.backendState) {
      SupportBackendState.Free -> {
        DonationContent(listener)
      }
      SupportBackendState.PlayUnavailable -> {
      }
    }
  }
}

@Composable
@OptIn(ExperimentalMaterial3ExpressiveApi::class)
private fun DonationContent(listener: SupportListener) {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    SupportHeader()

    val size = ButtonDefaults.MediumContainerHeight
    Button(
      modifier = Modifier
        .fillMaxWidth()
        .heightIn(size),
      onClick = listener::openSupport,
      contentPadding = ButtonDefaults.contentPaddingFor(size, hasStartIcon = true),
      colors = ButtonDefaults.buttonColors(
        containerColor = SupportLogoBlue,
        contentColor = Color.White,
      ),
    ) {
      Icon(
        imageVector = Icons.Outlined.Coffee,
        contentDescription = null,
        modifier = Modifier.size(ButtonDefaults.iconSizeFor(size)),
      )
      Spacer(Modifier.size(ButtonDefaults.iconSpacingFor(size)))
      Text(
        text = stringResource(StringsR.string.support_action_donate_kofi),
        style = ButtonDefaults.textStyleFor(size),
      )
    }

    SupportHelpsList()
  }
}

@Composable
private fun SupportHeader() {
  Column(
    modifier = Modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    Row(
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      Box(
        modifier = Modifier
          .size(40.dp)
          .clip(CircleShape),
      ) {
        val iconInset = 1.5F
        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer(
              scaleX = iconInset,
              scaleY = iconInset,
            ),
        ) {
          Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(UiR.drawable.ic_launcher_background),
            contentDescription = null,
          )
          Image(
            modifier = Modifier.fillMaxSize(),
            painter = painterResource(UiR.drawable.ic_launcher_foreground),
            contentDescription = null,
          )
        }
      }
      Text(
        text = stringResource(StringsR.string.support_title),
        style = MaterialTheme.typography.titleLarge,
      )
    }
    Text(
      text = stringResource(StringsR.string.support_description_maintenance),
      style = MaterialTheme.typography.bodyLarge,
    )
    Text(
      text = stringResource(StringsR.string.support_description_maintenance_subtitle),
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
  }
}

@Composable
private fun SupportHelpsList() {
  Column(
    modifier = Modifier.fillMaxWidth(),
  ) {
    Text(
      text = stringResource(StringsR.string.support_helps_title),
      style = MaterialTheme.typography.labelLarge,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    Spacer(Modifier.size(4.dp))
    SupportInfoRow(
      icon = Icons.Outlined.Construction,
      title = stringResource(StringsR.string.support_helps_maintenance),
    )
    HorizontalDivider()
    SupportInfoRow(
      icon = Icons.Outlined.AutoAwesome,
      title = stringResource(StringsR.string.support_helps_features),
    )
    HorizontalDivider()
    SupportInfoRow(
      icon = Icons.Outlined.LockOpen,
      title = stringResource(StringsR.string.support_helps_open_source),
    )
  }
}

@Composable
private fun SupportInfoRow(
  icon: ImageVector,
  title: String,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(vertical = 10.dp),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Icon(
      modifier = Modifier.size(22.dp),
      imageVector = icon,
      contentDescription = null,
      tint = MaterialTheme.colorScheme.primary,
    )
    Text(
      text = title,
      style = MaterialTheme.typography.bodyLarge,
    )
  }
}

@ContributesTo(AppScope::class)
interface SupportGraph {
  val supportViewModel: SupportViewModel
}

@ContributesTo(AppScope::class)
interface SupportProvider {

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

private val SupportLogoBlue = Color(0xFF0F4687)
