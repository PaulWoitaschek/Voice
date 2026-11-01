package voice.core.audiobookshelf.account

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.ContributesBinding
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import voice.core.analytics.api.Analytics
import voice.core.audiobookshelf.AudiobookshelfConnection
import voice.core.audiobookshelf.LibrariesPreview
import voice.core.audiobookshelf.ServerAccount
import voice.core.audiobookshelf.download.AudiobookshelfDownloads
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.login.LibrariesPreviews
import voice.core.audiobookshelf.login.serverName
import voice.core.audiobookshelf.sync.AudiobookshelfSync
import voice.core.audiobookshelf.sync.ProgressSync
import voice.core.data.isRemote
import voice.core.data.repo.BookContentRepo
import voice.core.data.store.CurrentBookStore
import voice.core.playback.PlayerController

@SingleIn(AppScope::class)
@ContributesBinding(AppScope::class)
@Inject
class AudiobookshelfAccount
internal constructor(
  private val accountStore: AccountStore,
  private val http: AudiobookshelfHttp,
  private val previews: LibrariesPreviews,
  private val sync: AudiobookshelfSync,
  private val progressSync: ProgressSync,
  private val downloads: AudiobookshelfDownloads,
  private val contentRepo: BookContentRepo,
  private val currentBookStore: CurrentBookStore,
  private val playerController: PlayerController,
  private val analytics: Analytics,
) : ServerAccount {

  override val connection: Flow<AudiobookshelfConnection?> = accountStore.data.map { it?.toConnection() }.distinctUntilChanged()

  override val serverName: Flow<String?> = connection.map { it?.serverName }.distinctUntilChanged()

  override suspend fun isConnected(): Boolean = accountStore.data.first() != null

  override suspend fun libraries(): LibrariesPreview? {
    val account = accountStore.data.first() ?: return null
    return previews.load(account, stored = true)
  }

  override suspend fun setLibraries(libraryIds: List<String>) {
    accountStore.updateData { it?.copy(libraryIds = libraryIds) }
    sync.sync(force = true)
  }

  override suspend fun signOut() {
    val account = accountStore.data.first() ?: return
    analytics.event("audiobookshelf_signed_out")
    // without the account no sync starts anymore, and the one that may still run would bring the books back
    accountStore.updateData { null }
    sync.cancel()
    if (currentBookStore.data.first()?.isRemote == true) {
      playerController.pause()
      currentBookStore.updateData { null }
    }
    downloads.removeAll()
    val remoteBooks = contentRepo.all().filter { it.id.isRemote }
    remoteBooks.forEach { contentRepo.put(it.copy(isActive = false)) }
    progressSync.forget(remoteBooks.map { it.id }.toSet())
    sync.onSignedOut()
    // last, as a server that can't be reached takes a while to answer
    try {
      @Suppress("RETURN_VALUE_NOT_USED") http.api(account.serverUrl).logout(account.refreshToken)
    } catch (_: IOException) {}
  }
}

private fun Account.toConnection() =
  AudiobookshelfConnection(
    serverName = serverName(serverUrl),
    username = username,
    libraryIds = libraryIds,
    needsLogin = needsLogin,
  )
