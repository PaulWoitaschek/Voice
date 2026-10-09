package voice.features.audiobookshelf.login

import java.io.File

internal sealed interface AudiobookshelfLoginViewState {

  data class Address(
    val address: String = "",
    val busy: Boolean = false,
    val error: AddressError? = null,
  ) : AudiobookshelfLoginViewState

  data class Credentials(
    val serverName: String,
    val username: String = "",
    val password: String = "",
    val passwordLogin: Boolean = true,
    val busy: Boolean = false,
    val error: CredentialsError? = null,
  ) : AudiobookshelfLoginViewState

  data class Libraries(
    val serverName: String,
    val loading: Boolean,
    val libraries: List<LibraryViewState> = emptyList(),
    val covers: List<File> = emptyList(),
    val busy: Boolean = false,
    /** The server didn't answer with its libraries. */
    val failed: Boolean = false,
  ) : AudiobookshelfLoginViewState {
    val bookCount: Int get() = libraries.filter { it.selected }.sumOf { it.bookCount }
    val canConnect: Boolean get() = !loading && !busy && libraries.any { it.selected }
  }
}

internal data class LibraryViewState(
  val id: String,
  val name: String,
  val bookCount: Int,
  val selected: Boolean,
)

internal sealed interface AddressError {
  data object NotFound : AddressError
  data object CertificateNotTrusted : AddressError
  data object NotSetUp : AddressError
  data class TooOld(val version: String) : AddressError
}

internal enum class CredentialsError {
  WrongCredentials,
  Unreachable,
  Failed,

  /** Signing in again only renews the login, another account needs a sign out first. */
  OtherAccount,
}
