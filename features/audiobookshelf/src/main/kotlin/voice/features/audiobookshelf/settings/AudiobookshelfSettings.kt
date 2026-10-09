@file:OptIn(ExperimentalMaterial3ExpressiveApi::class, ExperimentalMaterial3Api::class)

package voice.features.audiobookshelf.settings

import android.text.format.Formatter
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LargeFlexibleTopAppBar
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialShapes
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SheetValue.Expanded
import androidx.compose.material3.SheetValue.Hidden
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import coil.compose.AsyncImage
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.launch
import voice.core.audiobookshelf.download.BookDownloadState
import voice.core.audiobookshelf.download.WaitingFor
import voice.core.common.rootGraphAs
import voice.core.data.BookId
import voice.core.ui.AuroraBackground
import voice.core.ui.ShapedIcon
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.core.ui.plus
import voice.core.ui.rememberAnimationClock
import voice.core.ui.rememberCoverThumbnailRequest
import voice.core.ui.segmentedShape
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import java.text.NumberFormat
import voice.core.strings.R as StringsR
import voice.core.ui.R as UiR

@ContributesTo(AppScope::class)
interface AudiobookshelfSettingsGraph {
  val audiobookshelfSettingsViewModel: AudiobookshelfSettingsViewModel
}

@BindingContainer
@ContributesTo(AppScope::class)
object AudiobookshelfSettingsProvider {

  @Provides
  @IntoSet
  fun audiobookshelfSettingsNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.AudiobookshelfSettings> { key ->
    NavEntry(key) {
      AudiobookshelfSettings()
    }
  }
}

@Composable
private fun AudiobookshelfSettings() {
  val viewModel = retain { rootGraphAs<AudiobookshelfSettingsGraph>().audiobookshelfSettingsViewModel }
  val viewState = viewModel.viewState()
  AudiobookshelfSettings(
    viewState = viewState,
    onBack = viewModel::onBack,
    onLibraryToggle = { id -> viewState?.let { viewModel.onLibraryToggle(id, it.selectedLibraryIds) } },
    onDownloadOverMobileDataChange = viewModel::onDownloadOverMobileDataChange,
    onRemoveDownload = viewModel::onRemoveDownload,
    onRetryDownload = viewModel::onRetryDownload,
    onRemoveDownloads = viewModel::onRemoveDownloads,
    onSignInAgain = viewModel::onSignInAgain,
    onSyncNow = viewModel::onSyncNow,
    onSignOut = viewModel::onSignOut,
    onDismissSignOut = viewModel::onDismissSignOut,
    onConfirmSignOut = viewModel::onConfirmSignOut,
  )
}

@Composable
private fun AudiobookshelfSettings(
  viewState: AudiobookshelfSettingsViewState?,
  onBack: () -> Unit,
  onLibraryToggle: (String) -> Unit,
  onDownloadOverMobileDataChange: (Boolean) -> Unit,
  onRemoveDownload: (BookId) -> Unit,
  onRetryDownload: (BookId) -> Unit,
  onRemoveDownloads: () -> Unit,
  onSignInAgain: () -> Unit,
  onSyncNow: () -> Unit,
  onSignOut: () -> Unit,
  onDismissSignOut: () -> Unit,
  onConfirmSignOut: () -> Unit,
) {
  val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
  val clock = rememberAnimationClock(running = true)
  Box(Modifier.fillMaxSize()) {
    AuroraBackground(
      clock = { clock.value },
      showStars = false,
      modifier = Modifier.fillMaxSize(),
    )
    Scaffold(
      modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
      containerColor = Color.Transparent,
      contentColor = MaterialTheme.colorScheme.onSurface,
      topBar = {
        LargeFlexibleTopAppBar(
          title = { Text(stringResource(StringsR.string.audiobookshelf_title)) },
          navigationIcon = {
            val rtl = LocalLayoutDirection.current == LayoutDirection.Rtl
            IconButton(onClick = onBack) {
              Icon(
                modifier = Modifier.graphicsLayer { scaleX = if (rtl) -1F else 1F },
                imageVector = VoiceIcons.ArrowBack,
                contentDescription = stringResource(StringsR.string.common_action_back),
              )
            }
          },
          colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Transparent,
            scrolledContainerColor = MaterialTheme.colorScheme.surfaceContainer,
          ),
          scrollBehavior = scrollBehavior,
        )
      },
    ) { contentPadding ->
      if (viewState == null) return@Scaffold
      Column(
        modifier = Modifier
          .fillMaxSize()
          .verticalScroll(rememberScrollState())
          .padding(contentPadding + PaddingValues(start = 16.dp, end = 16.dp, top = 8.dp, bottom = 32.dp)),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp),
      ) {
        ServerIsland(viewState, onSignInAgain = onSignInAgain, onSyncNow = onSyncNow)
        if (!viewState.librariesUnavailable) {
          LibrariesIsland(viewState, onLibraryToggle)
        }
        DownloadsIsland(
          viewState = viewState,
          onDownloadOverMobileDataChange = onDownloadOverMobileDataChange,
          onRemoveDownload = onRemoveDownload,
          onRetryDownload = onRetryDownload,
          onRemoveDownloads = onRemoveDownloads,
        )
        OutlinedButton(
          onClick = onSignOut,
          shapes = ButtonDefaults.shapes(),
          colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
          modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
          Icon(VoiceIcons.Logout, contentDescription = null, modifier = Modifier.size(ButtonDefaults.IconSize))
          Spacer(Modifier.width(ButtonDefaults.IconSpacing))
          Text(stringResource(StringsR.string.audiobookshelf_settings_sign_out))
        }
      }
      if (viewState.confirmSignOut) {
        SignOutSheet(
          serverName = viewState.serverName,
          onDismiss = onDismissSignOut,
          onConfirm = onConfirmSignOut,
        )
      }
    }
  }
}

@Composable
private fun Island(
  title: String?,
  modifier: Modifier = Modifier,
  content: @Composable ColumnScope.() -> Unit,
) {
  Column(
    modifier = modifier
      .widthIn(max = 520.dp)
      .fillMaxWidth()
      .background(MaterialTheme.colorScheme.surfaceContainer, RoundedCornerShape(32.dp))
      .padding(20.dp),
  ) {
    if (title != null) {
      Text(
        modifier = Modifier.semantics { heading() },
        text = title,
        style = MaterialTheme.typography.titleMediumEmphasized,
      )
      Spacer(Modifier.height(12.dp))
    }
    content()
  }
}

@Composable
private fun ServerIsland(
  viewState: AudiobookshelfSettingsViewState,
  onSignInAgain: () -> Unit,
  onSyncNow: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  Island(title = null) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      ShapedIcon(
        icon = VoiceIcons.Dns,
        shape = MaterialShapes.Cookie9Sided,
        containerColor = colors.primary,
        contentColor = colors.onPrimary,
        size = 56.dp,
      )
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = viewState.serverName,
          style = MaterialTheme.typography.titleLarge,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
        Text(
          text = stringResource(StringsR.string.audiobookshelf_settings_signed_in_as, viewState.username),
          style = MaterialTheme.typography.bodyMedium,
          color = colors.onSurfaceVariant,
        )
      }
    }
    Spacer(Modifier.height(16.dp))
    val (icon, text) = when (viewState.status) {
      ServerStatus.Connected -> VoiceIcons.CloudDone to StringsR.string.audiobookshelf_status_connected
      ServerStatus.Syncing -> VoiceIcons.Sync to StringsR.string.audiobookshelf_status_syncing
      ServerStatus.Offline -> VoiceIcons.CloudOff to StringsR.string.audiobookshelf_status_offline
      ServerStatus.NeedsLogin -> VoiceIcons.Person to StringsR.string.audiobookshelf_status_needs_login
    }
    val needsLogin = viewState.status == ServerStatus.NeedsLogin
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .background(
          if (needsLogin) colors.errorContainer else colors.secondaryContainer,
          RoundedCornerShape(20.dp),
        )
        .padding(horizontal = 16.dp, vertical = 12.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      val contentColor = if (needsLogin) colors.onErrorContainer else colors.onSecondaryContainer
      if (viewState.status == ServerStatus.Syncing) {
        LoadingIndicator(Modifier.size(24.dp), color = contentColor)
      } else {
        Icon(icon, contentDescription = null, tint = contentColor)
      }
      Spacer(Modifier.width(12.dp))
      Text(
        modifier = Modifier.weight(1F),
        text = stringResource(text),
        style = MaterialTheme.typography.bodyMedium,
        color = contentColor,
      )
      when (viewState.status) {
        ServerStatus.NeedsLogin -> TextButton(onClick = onSignInAgain) {
          Text(stringResource(StringsR.string.audiobookshelf_settings_sign_in_again))
        }
        ServerStatus.Offline -> TextButton(onClick = onSyncNow) {
          Text(stringResource(StringsR.string.audiobookshelf_settings_try_again))
        }
        ServerStatus.Connected, ServerStatus.Syncing -> Unit
      }
    }
  }
}

@Composable
private fun LibrariesIsland(
  viewState: AudiobookshelfSettingsViewState,
  onLibraryToggle: (String) -> Unit,
) {
  Island(title = stringResource(StringsR.string.audiobookshelf_settings_libraries)) {
    val libraries = viewState.libraries
    if (libraries == null) {
      Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        LoadingIndicator(Modifier.size(40.dp))
      }
      return@Island
    }
    FlowRow(
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
      libraries.forEach { library ->
        FilterChip(
          selected = library.selected,
          onClick = { onLibraryToggle(library.id) },
          leadingIcon = if (library.selected) {
            { Icon(VoiceIcons.Check, contentDescription = null, modifier = Modifier.size(18.dp)) }
          } else {
            null
          },
          label = {
            Text(
              pluralStringResource(
                StringsR.plurals.audiobookshelf_login_library_chip,
                library.bookCount,
                library.name,
                library.bookCount,
              ),
            )
          },
        )
      }
    }
  }
}

@Composable
private fun DownloadsIsland(
  viewState: AudiobookshelfSettingsViewState,
  onDownloadOverMobileDataChange: (Boolean) -> Unit,
  onRemoveDownload: (BookId) -> Unit,
  onRetryDownload: (BookId) -> Unit,
  onRemoveDownloads: () -> Unit,
) {
  val context = LocalContext.current
  Island(title = stringResource(StringsR.string.audiobookshelf_settings_downloads)) {
    Row(verticalAlignment = Alignment.CenterVertically) {
      Icon(VoiceIcons.DownloadDone, contentDescription = null)
      Spacer(Modifier.width(16.dp))
      Text(
        modifier = Modifier.weight(1F),
        text = if (viewState.usedBytes > 0) {
          stringResource(
            StringsR.string.audiobookshelf_settings_downloads_used,
            Formatter.formatShortFileSize(context, viewState.usedBytes),
          )
        } else {
          stringResource(StringsR.string.audiobookshelf_settings_downloads_none)
        },
        style = MaterialTheme.typography.bodyLarge,
      )
      if (viewState.usedBytes > 0) {
        TextButton(onClick = onRemoveDownloads) {
          Text(stringResource(StringsR.string.audiobookshelf_settings_downloads_remove))
        }
      }
    }
    if (viewState.downloads.isNotEmpty()) {
      Spacer(Modifier.height(12.dp))
      Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        viewState.downloads.forEachIndexed { index, download ->
          DownloadRow(
            download = download,
            shape = segmentedShape(index, viewState.downloads.size),
            onRemove = { onRemoveDownload(download.bookId) },
            onRetry = { onRetryDownload(download.bookId) },
          )
        }
      }
    }
    Spacer(Modifier.height(8.dp))
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .toggleable(
          value = viewState.downloadOverMobileData,
          role = Role.Switch,
          onValueChange = onDownloadOverMobileDataChange,
        )
        .heightIn(min = 56.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Icon(VoiceIcons.NetworkCell, contentDescription = null)
      Spacer(Modifier.width(16.dp))
      Column(Modifier.weight(1F)) {
        Text(
          text = stringResource(StringsR.string.audiobookshelf_settings_mobile_data),
          style = MaterialTheme.typography.bodyLarge,
        )
        Text(
          text = stringResource(StringsR.string.audiobookshelf_settings_mobile_data_summary),
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
      Spacer(Modifier.width(12.dp))
      Switch(checked = viewState.downloadOverMobileData, onCheckedChange = null)
    }
  }
}

@Composable
private fun DownloadRow(
  download: DownloadViewState,
  shape: Shape,
  onRemove: () -> Unit,
  onRetry: () -> Unit,
) {
  val colors = MaterialTheme.colorScheme
  val context = LocalContext.current
  val state = download.state
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .background(colors.surfaceContainerHigh, shape)
      .heightIn(min = 72.dp)
      .padding(start = 12.dp, top = 8.dp, bottom = 8.dp, end = 4.dp),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    AsyncImage(
      modifier = Modifier
        .size(48.dp)
        .clip(RoundedCornerShape(percent = 12)),
      model = rememberCoverThumbnailRequest(download.cover),
      placeholder = ColorPainter(colors.surfaceContainerHighest),
      error = painterResource(UiR.drawable.album_art),
      contentScale = ContentScale.Crop,
      contentDescription = null,
    )
    Spacer(Modifier.width(16.dp))
    Column(Modifier.weight(1F)) {
      Text(
        text = download.name,
        style = MaterialTheme.typography.bodyLarge,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      Text(
        text = when (state) {
          is BookDownloadState.Downloading -> when (state.waitingFor) {
            WaitingFor.Wifi -> stringResource(StringsR.string.book_download_waiting_for_wifi)
            WaitingFor.Connection -> stringResource(StringsR.string.book_download_waiting_for_connection)
            null -> if (state.totalBytes > 0) {
              stringResource(
                StringsR.string.book_download_progress,
                Formatter.formatShortFileSize(context, state.downloadedBytes),
                Formatter.formatShortFileSize(context, state.totalBytes),
              )
            } else {
              NumberFormat.getPercentInstance().format(state.progress)
            }
          }
          is BookDownloadState.Downloaded -> Formatter.formatShortFileSize(context, state.bytes)
          BookDownloadState.Failed -> stringResource(StringsR.string.book_download_failed)
          BookDownloadState.NotDownloaded -> ""
        },
        style = MaterialTheme.typography.bodyMedium.copy(fontFeatureSettings = "tnum"),
        color = if (state == BookDownloadState.Failed) colors.error else colors.onSurfaceVariant,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
      )
      if (state is BookDownloadState.Downloading) {
        val progress by animateFloatAsState(state.progress, label = "downloadProgress")
        Spacer(Modifier.height(6.dp))
        LinearProgressIndicator(
          progress = { progress },
          modifier = Modifier.fillMaxWidth(),
          color = if (state.waitingFor != null) colors.onSurfaceVariant else colors.primary,
        )
      }
    }
    Spacer(Modifier.width(4.dp))
    if (state == BookDownloadState.Failed) {
      IconButton(onClick = onRetry) {
        Icon(VoiceIcons.Replay, contentDescription = stringResource(StringsR.string.book_download_retry))
      }
    }
    IconButton(onClick = onRemove) {
      if (state is BookDownloadState.Downloading) {
        Icon(VoiceIcons.Close, contentDescription = stringResource(StringsR.string.book_download_stop))
      } else {
        Icon(VoiceIcons.Delete, contentDescription = stringResource(StringsR.string.book_download_remove))
      }
    }
  }
}

@Composable
private fun SignOutSheet(
  serverName: String,
  onDismiss: () -> Unit,
  onConfirm: () -> Unit,
) {
  val sheetState = rememberBottomSheetState(
    initialValue = Hidden,
    enabledValues = setOf(Hidden, Expanded),
  )
  val scope = rememberCoroutineScope()
  var closing by remember { mutableStateOf(false) }
  val hideThen: (() -> Unit) -> Unit = { action ->
    if (!closing) {
      closing = true
      scope.launch {
        try {
          sheetState.hide()
        } finally {
          action()
        }
      }
    }
  }
  val colors = MaterialTheme.colorScheme
  ModalBottomSheet(
    onDismissRequest = { if (!closing) onDismiss() },
    sheetState = sheetState,
  ) {
    Column(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState())
        .navigationBarsPadding()
        .padding(start = 24.dp, end = 24.dp, bottom = 16.dp),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      ShapedIcon(
        icon = VoiceIcons.Logout,
        shape = MaterialShapes.Cookie9Sided,
        containerColor = colors.errorContainer,
        contentColor = colors.onErrorContainer,
        size = 72.dp,
      )
      Spacer(Modifier.height(20.dp))
      Text(
        text = stringResource(StringsR.string.audiobookshelf_sign_out_title, serverName),
        style = MaterialTheme.typography.headlineSmallEmphasized,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(12.dp))
      Text(
        text = stringResource(StringsR.string.audiobookshelf_sign_out_message),
        style = MaterialTheme.typography.bodyMedium,
        color = colors.onSurfaceVariant,
        textAlign = TextAlign.Center,
      )
      Spacer(Modifier.height(24.dp))
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
      ) {
        OutlinedButton(
          onClick = { hideThen(onDismiss) },
          shapes = ButtonDefaults.shapes(),
          modifier = Modifier
            .weight(1F)
            .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
          Text(stringResource(StringsR.string.common_dialog_cancel))
        }
        Button(
          onClick = { hideThen(onConfirm) },
          shapes = ButtonDefaults.shapes(),
          colors = ButtonDefaults.buttonColors(
            containerColor = colors.error,
            contentColor = colors.onError,
          ),
          modifier = Modifier
            .weight(1F)
            .heightIn(min = ButtonDefaults.MediumContainerHeight),
        ) {
          Text(stringResource(StringsR.string.audiobookshelf_settings_sign_out))
        }
      }
    }
  }
}

@Composable
@Preview
private fun AudiobookshelfSettingsPreview() {
  VoiceTheme {
    AudiobookshelfSettings(
      viewState = AudiobookshelfSettingsViewState(
        serverName = "audiobooks.example.com",
        username = "paul",
        status = ServerStatus.Connected,
        libraries = listOf(
          LibraryViewState(id = "1", name = "Audiobooks", bookCount = 132, selected = true),
          LibraryViewState(id = "2", name = "Kids", bookCount = 24, selected = false),
        ),
        usedBytes = 1_234_567_890,
        downloads = listOf(
          DownloadViewState(
            bookId = BookId("abs://item/1"),
            name = "The Hobbit",
            cover = null,
            state = BookDownloadState.Downloading(
              progress = 0.4F,
              downloadedBytes = 120_000_000,
              totalBytes = 300_000_000,
              waitingFor = null,
            ),
          ),
          DownloadViewState(
            bookId = BookId("abs://item/2"),
            name = "Dune",
            cover = null,
            state = BookDownloadState.Downloaded(bytes = 812_000_000),
          ),
        ),
        downloadOverMobileData = false,
        confirmSignOut = false,
      ),
      onBack = {},
      onLibraryToggle = {},
      onDownloadOverMobileDataChange = {},
      onRemoveDownload = {},
      onRetryDownload = {},
      onRemoveDownloads = {},
      onSignInAgain = {},
      onSyncNow = {},
      onSignOut = {},
      onDismissSignOut = {},
      onConfirmSignOut = {},
    )
  }
}
