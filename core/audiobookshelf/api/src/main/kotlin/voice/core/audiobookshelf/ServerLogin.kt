package voice.core.audiobookshelf

import java.io.File

interface ServerLogin {

  suspend fun findServer(address: String): FindServerResult

  suspend fun login(
    server: AudiobookshelfServer,
    username: String,
    password: String,
  ): LoginResult

  suspend fun preview(login: PendingLogin): LibrariesPreview?

  /**
   * Stores the login, which brings the books of [libraryIds] into the library. Another account that
   * is connected is signed out first, as it would hand its books, downloads and positions to this
   * one.
   */
  suspend fun connect(
    login: PendingLogin,
    libraryIds: List<String>,
  )

  /**
   * Signs in again after the server declined the stored login. Returns false for another account,
   * which would take over the books, downloads and positions of this one.
   */
  suspend fun renewLogin(login: PendingLogin): Boolean
}

data class AudiobookshelfServer(
  val url: String,
  val version: String,
  val passwordLogin: Boolean,
)

sealed interface FindServerResult {
  data class Found(val server: AudiobookshelfServer) : FindServerResult

  data object NotFound : FindServerResult

  data object CertificateNotTrusted : FindServerResult

  data object NotSetUp : FindServerResult

  data class TooOld(val version: String) : FindServerResult
}

/** A successful login that isn't stored yet, so the listener can still pick the libraries. */
interface PendingLogin {
  val serverName: String
}

sealed interface LoginResult {
  data class Success(val login: PendingLogin) : LoginResult

  data object WrongCredentials : LoginResult

  data object Unreachable : LoginResult

  data object Failed : LoginResult
}

data class AudiobookshelfLibrary(
  val id: String,
  val name: String,
  val bookCount: Int,
)

data class LibrariesPreview(
  val libraries: List<AudiobookshelfLibrary>,
  val covers: List<File>,
)
