package voice.features.bookOverview.overview

import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.Inject
import dev.zacsweers.metro.SingleIn
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import voice.core.analytics.api.Analytics
import voice.core.common.DispatcherProvider
import voice.core.data.folders.AudiobookFolders
import voice.core.data.repo.BookContentRepo

// app scoped, as the library screen is created again when coming back to it
@SingleIn(AppScope::class)
@Inject
class EmptyLibraryTracker(
  private val analytics: Analytics,
  private val audiobookFolders: AudiobookFolders,
  private val bookContentRepo: BookContentRepo,
  private val dispatcherProvider: DispatcherProvider,
) {

  private var tracked = false

  // tells apart folders that lost access, books that vanished from their folders and a library that never had any (#3965)
  suspend fun track() {
    if (tracked) return
    tracked = true
    val params = withContext(dispatcherProvider.io) {
      mapOf(
        "folders" to audiobookFolders.all().first().values.sumOf { it.size }.toString(),
        "folders_without_access" to audiobookFolders.withoutAccess().first().values.sumOf { it.size }.toString(),
        "inactive_books" to bookContentRepo.all().count { !it.isActive }.toString(),
      )
    }
    analytics.event("library_empty", params)
  }
}
