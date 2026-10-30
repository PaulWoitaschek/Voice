package voice.core.audiobookshelf

import kotlinx.coroutines.flow.Flow

interface ServerAccount {

  val connection: Flow<AudiobookshelfConnection?>

  /** The connected server as people know it, or null without one. */
  val serverName: Flow<String?>

  suspend fun isConnected(): Boolean

  suspend fun libraries(): LibrariesPreview?

  suspend fun setLibraries(libraryIds: List<String>)

  /**
   * Removes the server books and their downloads from Voice. Their progress lives on on the server.
   */
  suspend fun signOut()
}

data class AudiobookshelfConnection(
  /** The server as people know it, without the scheme and the trailing slash. */
  val serverName: String,
  val username: String,
  val libraryIds: List<String>,
  val needsLogin: Boolean,
)
