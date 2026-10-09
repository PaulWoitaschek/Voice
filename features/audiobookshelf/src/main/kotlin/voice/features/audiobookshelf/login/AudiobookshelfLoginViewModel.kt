package voice.features.audiobookshelf.login

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import voice.core.audiobookshelf.Audiobookshelf
import voice.core.audiobookshelf.AudiobookshelfServer
import voice.core.audiobookshelf.FindServerResult
import voice.core.audiobookshelf.LoginResult
import voice.core.audiobookshelf.PendingLogin
import voice.core.common.DispatcherProvider
import voice.core.common.MainScope
import voice.navigation.Destination
import voice.navigation.Navigator
import voice.navigation.Origin

@AssistedInject
class AudiobookshelfLoginViewModel(
  private val audiobookshelf: Audiobookshelf,
  private val navigator: Navigator,
  dispatcherProvider: DispatcherProvider,
  @Assisted
  private val origin: Origin,
  @Assisted
  private val renew: Boolean,
) {

  private val scope = MainScope(dispatcherProvider)

  private var server: AudiobookshelfServer? = null
  private var login: PendingLogin? = null
  private var knownUsername = ""

  internal var state: AudiobookshelfLoginViewState by mutableStateOf(AudiobookshelfLoginViewState.Address())
    private set

  init {
    if (renew) {
      scope.launch {
        val connection = audiobookshelf.connection.first() ?: return@launch
        knownUsername = connection.username
        state = AudiobookshelfLoginViewState.Address(address = connection.serverName)
        onFindServer()
      }
    }
  }

  internal fun onAddressChange(address: String) {
    val current = state as? AudiobookshelfLoginViewState.Address ?: return
    state = current.copy(address = address, error = null)
  }

  internal fun onFindServer() {
    val current = state as? AudiobookshelfLoginViewState.Address ?: return
    if (current.busy || current.address.isBlank()) return
    state = current.copy(busy = true, error = null)
    scope.launch {
      when (val result = audiobookshelf.findServer(current.address)) {
        is FindServerResult.Found -> {
          server = result.server
          state = AudiobookshelfLoginViewState.Credentials(
            serverName = current.address.trim().removeSuffix("/"),
            username = knownUsername,
            passwordLogin = result.server.passwordLogin,
          )
        }
        FindServerResult.NotFound -> state = current.copy(error = AddressError.NotFound)
        FindServerResult.CertificateNotTrusted -> state = current.copy(error = AddressError.CertificateNotTrusted)
        FindServerResult.NotSetUp -> state = current.copy(error = AddressError.NotSetUp)
        is FindServerResult.TooOld -> state = current.copy(error = AddressError.TooOld(result.version))
      }
    }
  }

  internal fun onUsernameChange(username: String) {
    val current = state as? AudiobookshelfLoginViewState.Credentials ?: return
    state = current.copy(username = username, error = null)
  }

  internal fun onPasswordChange(password: String) {
    val current = state as? AudiobookshelfLoginViewState.Credentials ?: return
    state = current.copy(password = password, error = null)
  }

  internal fun onLogin() {
    val current = state as? AudiobookshelfLoginViewState.Credentials ?: return
    val server = server ?: return
    if (current.busy || current.username.isBlank()) return
    state = current.copy(busy = true, error = null)
    scope.launch {
      when (val result = audiobookshelf.login(server, current.username, current.password)) {
        is LoginResult.Success -> {
          login = result.login
          if (renew) {
            audiobookshelf.renewLogin(result.login)
            navigator.goBack()
          } else {
            showLibraries(result.login, current.serverName)
          }
        }
        LoginResult.WrongCredentials -> state = current.copy(error = CredentialsError.WrongCredentials)
        LoginResult.Unreachable -> state = current.copy(error = CredentialsError.Unreachable)
        LoginResult.Failed -> state = current.copy(error = CredentialsError.Failed)
      }
    }
  }

  private suspend fun showLibraries(
    login: PendingLogin,
    serverName: String,
  ) {
    state = AudiobookshelfLoginViewState.Libraries(serverName = serverName, loading = true)
    val preview = audiobookshelf.preview(login)
    state = AudiobookshelfLoginViewState.Libraries(
      serverName = serverName,
      loading = false,
      libraries = preview?.libraries.orEmpty().map { library ->
        LibraryViewState(
          id = library.id,
          name = library.name,
          bookCount = library.bookCount,
          selected = true,
        )
      },
      covers = preview?.covers.orEmpty(),
    )
  }

  internal fun onLibraryToggle(id: String) {
    val current = state as? AudiobookshelfLoginViewState.Libraries ?: return
    state = current.copy(
      libraries = current.libraries.map {
        if (it.id == id) it.copy(selected = !it.selected) else it
      },
    )
  }

  internal fun onConnect() {
    val current = state as? AudiobookshelfLoginViewState.Libraries ?: return
    val login = login ?: return
    if (current.busy) return
    state = current.copy(busy = true)
    scope.launch {
      audiobookshelf.connect(login, current.libraries.filter { it.selected }.map { it.id })
      when (origin) {
        Origin.Default -> navigator.setRoot(Destination.BookOverview)
        Origin.Onboarding -> navigator.goTo(Destination.OnboardingCompletion)
      }
    }
  }

  internal fun onBack() {
    state = when (val current = state) {
      is AudiobookshelfLoginViewState.Address -> {
        navigator.goBack()
        return
      }
      is AudiobookshelfLoginViewState.Credentials -> AudiobookshelfLoginViewState.Address(address = current.serverName)
      is AudiobookshelfLoginViewState.Libraries -> AudiobookshelfLoginViewState.Credentials(
        serverName = current.serverName,
        passwordLogin = server?.passwordLogin ?: true,
      )
    }
  }

  @AssistedFactory
  interface Factory {
    fun create(
      origin: Origin,
      renew: Boolean,
    ): AudiobookshelfLoginViewModel
  }
}
