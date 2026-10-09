@file:OptIn(ExperimentalMaterial3ExpressiveApi::class)

package voice.features.audiobookshelf.login

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.retain.retain
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.autofill.ContentType
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentType
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.ContributesTo
import dev.zacsweers.metro.IntoSet
import dev.zacsweers.metro.Provides
import voice.core.common.rootGraphAs
import voice.core.ui.OnboardingButton
import voice.core.ui.OnboardingScaffold
import voice.core.ui.OnboardingStep
import voice.core.ui.VoiceTheme
import voice.core.ui.icons.VoiceIcons
import voice.features.audiobookshelf.views.CoversHero
import voice.features.audiobookshelf.views.ServerHero
import voice.navigation.Destination
import voice.navigation.NavEntryProvider
import voice.navigation.Origin
import voice.core.strings.R as StringsR

@ContributesTo(AppScope::class)
interface AudiobookshelfLoginGraph {
  val audiobookshelfLoginViewModelFactory: AudiobookshelfLoginViewModel.Factory
}

@BindingContainer
@ContributesTo(AppScope::class)
object AudiobookshelfLoginProvider {

  @Provides
  @IntoSet
  fun audiobookshelfLoginNavEntryProvider(): NavEntryProvider<*> = NavEntryProvider<Destination.AudiobookshelfLogin> { key ->
    NavEntry(key) {
      AudiobookshelfLogin(origin = key.origin, renew = key.renew)
    }
  }
}

@Composable
private fun AudiobookshelfLogin(
  origin: Origin,
  renew: Boolean,
) {
  val viewModel = retain(origin.name, renew) {
    rootGraphAs<AudiobookshelfLoginGraph>().audiobookshelfLoginViewModelFactory.create(origin, renew)
  }
  BackHandler(onBack = viewModel::onBack)
  AudiobookshelfLogin(
    state = viewModel.state,
    origin = origin,
    onBack = viewModel::onBack,
    onAddressChange = viewModel::onAddressChange,
    onFindServer = viewModel::onFindServer,
    onUsernameChange = viewModel::onUsernameChange,
    onPasswordChange = viewModel::onPasswordChange,
    onLogin = viewModel::onLogin,
    onLibraryToggle = viewModel::onLibraryToggle,
    onConnect = viewModel::onConnect,
    onRetryLibraries = viewModel::onRetryLibraries,
  )
}

@Composable
private fun AudiobookshelfLogin(
  state: AudiobookshelfLoginViewState,
  origin: Origin,
  onBack: () -> Unit,
  onAddressChange: (String) -> Unit,
  onFindServer: () -> Unit,
  onUsernameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLogin: () -> Unit,
  onLibraryToggle: (String) -> Unit,
  onConnect: () -> Unit,
  onRetryLibraries: () -> Unit,
) {
  val step = when (origin) {
    Origin.Default -> null
    Origin.Onboarding -> OnboardingStep.AddContent
  }
  when (state) {
    is AudiobookshelfLoginViewState.Address -> OnboardingScaffold(
      step = step,
      onBack = onBack,
      title = stringResource(StringsR.string.audiobookshelf_login_address_title),
      subtitle = stringResource(StringsR.string.audiobookshelf_login_address_subtitle),
      hero = { clock -> ServerHero(clock = clock, modifier = Modifier.fillMaxSize()) },
      details = { AddressDetails(state, onAddressChange, onFindServer) },
      actions = {
        BusyButton(
          text = stringResource(StringsR.string.audiobookshelf_login_address_action),
          busy = state.busy,
          enabled = state.address.isNotBlank(),
          onClick = onFindServer,
        )
      },
    )
    is AudiobookshelfLoginViewState.Credentials -> OnboardingScaffold(
      step = step,
      onBack = onBack,
      title = stringResource(StringsR.string.audiobookshelf_login_credentials_title),
      subtitle = if (state.passwordLogin) {
        stringResource(StringsR.string.audiobookshelf_login_credentials_subtitle, state.serverName)
      } else {
        stringResource(StringsR.string.audiobookshelf_login_credentials_no_password)
      },
      hero = { clock -> ServerHero(clock = clock, modifier = Modifier.fillMaxSize()) },
      details = {
        if (state.passwordLogin) {
          CredentialsDetails(state, onUsernameChange, onPasswordChange, onLogin)
        }
      },
      actions = {
        if (state.passwordLogin) {
          BusyButton(
            text = stringResource(StringsR.string.audiobookshelf_login_credentials_action),
            busy = state.busy,
            enabled = state.username.isNotBlank(),
            onClick = onLogin,
          )
        }
      },
    )
    is AudiobookshelfLoginViewState.Libraries -> OnboardingScaffold(
      step = step,
      onBack = onBack,
      title = when {
        state.loading -> stringResource(StringsR.string.audiobookshelf_login_libraries_loading)
        state.failed -> stringResource(StringsR.string.audiobookshelf_login_error_unreachable)
        state.libraries.isEmpty() -> stringResource(StringsR.string.audiobookshelf_login_libraries_none)
        else -> pluralStringResource(StringsR.plurals.audiobookshelf_login_libraries_title, state.bookCount, state.bookCount)
      },
      subtitle = stringResource(StringsR.string.audiobookshelf_login_libraries_subtitle),
      hero = { clock ->
        if (state.covers.isEmpty()) {
          ServerHero(clock = clock, modifier = Modifier.fillMaxSize())
        } else {
          CoversHero(covers = state.covers, modifier = Modifier.fillMaxSize())
        }
      },
      details = { LibrariesDetails(state, onLibraryToggle) },
      actions = {
        if (state.failed) {
          BusyButton(
            text = stringResource(StringsR.string.audiobookshelf_settings_try_again),
            busy = false,
            enabled = true,
            onClick = onRetryLibraries,
          )
        } else {
          BusyButton(
            text = stringResource(StringsR.string.audiobookshelf_login_libraries_action),
            busy = state.loading || state.busy,
            enabled = state.canConnect,
            onClick = onConnect,
          )
        }
      },
    )
  }
}

@Composable
private fun ColumnScope.AddressDetails(
  state: AudiobookshelfLoginViewState.Address,
  onAddressChange: (String) -> Unit,
  onFindServer: () -> Unit,
) {
  val focusRequester = remember { FocusRequester() }
  LaunchedEffect(focusRequester) { focusRequester.requestFocus() }
  Spacer(Modifier.height(24.dp))
  OutlinedTextField(
    modifier = Modifier
      .fillMaxWidth()
      .focusRequester(focusRequester),
    value = state.address,
    onValueChange = onAddressChange,
    enabled = !state.busy,
    singleLine = true,
    label = { Text(stringResource(StringsR.string.audiobookshelf_login_address_label)) },
    placeholder = { Text(stringResource(StringsR.string.audiobookshelf_login_address_placeholder)) },
    isError = state.error != null,
    supportingText = state.error?.let { error ->
      {
        Text(
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          text = error.message(),
        )
      }
    },
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Uri,
      imeAction = ImeAction.Go,
      autoCorrectEnabled = false,
      capitalization = KeyboardCapitalization.None,
    ),
    keyboardActions = KeyboardActions(onGo = { onFindServer() }),
  )
}

@Composable
private fun AddressError.message(): String = when (this) {
  AddressError.NotFound -> stringResource(StringsR.string.audiobookshelf_login_error_not_found)
  AddressError.CertificateNotTrusted -> stringResource(StringsR.string.audiobookshelf_login_error_certificate)
  AddressError.NotSetUp -> stringResource(StringsR.string.audiobookshelf_login_error_not_set_up)
  is AddressError.TooOld -> stringResource(StringsR.string.audiobookshelf_login_error_too_old, version)
}

@Composable
private fun ColumnScope.CredentialsDetails(
  state: AudiobookshelfLoginViewState.Credentials,
  onUsernameChange: (String) -> Unit,
  onPasswordChange: (String) -> Unit,
  onLogin: () -> Unit,
) {
  val passwordFocus = remember { FocusRequester() }
  val usernameFocus = remember { FocusRequester() }
  LaunchedEffect(Unit) {
    if (state.username.isEmpty()) usernameFocus.requestFocus() else passwordFocus.requestFocus()
  }
  var showPassword by rememberSaveable { mutableStateOf(false) }
  Spacer(Modifier.height(24.dp))
  OutlinedTextField(
    modifier = Modifier
      .fillMaxWidth()
      .focusRequester(usernameFocus)
      .semantics { contentType = ContentType.Username },
    value = state.username,
    onValueChange = onUsernameChange,
    enabled = !state.busy,
    singleLine = true,
    label = { Text(stringResource(StringsR.string.audiobookshelf_login_username)) },
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Text,
      imeAction = ImeAction.Next,
      autoCorrectEnabled = false,
      capitalization = KeyboardCapitalization.None,
    ),
    keyboardActions = KeyboardActions(onNext = { passwordFocus.requestFocus() }),
  )
  Spacer(Modifier.height(8.dp))
  OutlinedTextField(
    modifier = Modifier
      .fillMaxWidth()
      .focusRequester(passwordFocus)
      .semantics { contentType = ContentType.Password },
    value = state.password,
    onValueChange = onPasswordChange,
    enabled = !state.busy,
    singleLine = true,
    label = { Text(stringResource(StringsR.string.audiobookshelf_login_password)) },
    visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
    trailingIcon = {
      IconButton(onClick = { showPassword = !showPassword }) {
        Icon(
          imageVector = if (showPassword) VoiceIcons.VisibilityOff else VoiceIcons.Visibility,
          contentDescription = stringResource(
            if (showPassword) {
              StringsR.string.audiobookshelf_login_password_hide
            } else {
              StringsR.string.audiobookshelf_login_password_show
            },
          ),
        )
      }
    },
    isError = state.error != null,
    supportingText = state.error?.let { error ->
      {
        Text(
          modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
          text = stringResource(
            when (error) {
              CredentialsError.WrongCredentials -> StringsR.string.audiobookshelf_login_error_credentials
              CredentialsError.Unreachable -> StringsR.string.audiobookshelf_login_error_unreachable
              CredentialsError.Failed -> StringsR.string.audiobookshelf_login_error_failed
              CredentialsError.OtherAccount -> StringsR.string.audiobookshelf_login_error_other_account
            },
          ),
        )
      }
    },
    keyboardOptions = KeyboardOptions(
      keyboardType = KeyboardType.Password,
      imeAction = ImeAction.Done,
      autoCorrectEnabled = false,
    ),
    keyboardActions = KeyboardActions(onDone = { onLogin() }),
  )
}

@Composable
private fun ColumnScope.LibrariesDetails(
  state: AudiobookshelfLoginViewState.Libraries,
  onLibraryToggle: (String) -> Unit,
) {
  // with a single library there is nothing to choose
  if (state.libraries.size < 2) return
  Spacer(Modifier.height(24.dp))
  FlowRow(
    modifier = Modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    verticalArrangement = Arrangement.spacedBy(8.dp),
  ) {
    state.libraries.forEach { library ->
      FilterChip(
        selected = library.selected,
        onClick = { onLibraryToggle(library.id) },
        enabled = !state.busy,
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

@Composable
private fun BusyButton(
  text: String,
  busy: Boolean,
  enabled: Boolean,
  onClick: () -> Unit,
) {
  if (busy) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(56.dp),
      contentAlignment = Alignment.Center,
    ) {
      LoadingIndicator(Modifier.size(48.dp))
    }
  } else {
    OnboardingButton(
      text = text,
      onClick = onClick,
      enabled = enabled,
      trailingArrow = true,
    )
  }
}

@Composable
@Preview
private fun AddressPreview() {
  VoiceTheme {
    AudiobookshelfLogin(
      state = AudiobookshelfLoginViewState.Address(address = "audiobooks.example.com", error = AddressError.NotFound),
      origin = Origin.Onboarding,
      onBack = {},
      onAddressChange = {},
      onFindServer = {},
      onUsernameChange = {},
      onPasswordChange = {},
      onLogin = {},
      onLibraryToggle = {},
      onConnect = {},
      onRetryLibraries = {},
    )
  }
}

@Composable
@Preview
private fun LibrariesPreview() {
  VoiceTheme {
    AudiobookshelfLogin(
      state = AudiobookshelfLoginViewState.Libraries(
        serverName = "audiobooks.example.com",
        loading = false,
        libraries = listOf(
          LibraryViewState(id = "1", name = "Audiobooks", bookCount = 132, selected = true),
          LibraryViewState(id = "2", name = "Kids", bookCount = 24, selected = false),
        ),
      ),
      origin = Origin.Default,
      onBack = {},
      onAddressChange = {},
      onFindServer = {},
      onUsernameChange = {},
      onPasswordChange = {},
      onLogin = {},
      onLibraryToggle = {},
      onConnect = {},
      onRetryLibraries = {},
    )
  }
}
