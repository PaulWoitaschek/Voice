package voice.features.bookOverview.overview

import android.net.Uri
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import voice.core.analytics.api.Analytics
import voice.core.common.DispatcherProvider
import voice.core.data.folders.AudiobookFolders
import voice.core.data.folders.DocumentFileWithUri
import voice.core.data.folders.FolderType
import voice.core.data.repo.BookContentRepo
import voice.features.bookOverview.book
import kotlin.test.Test

class EmptyLibraryTrackerTest {

  private val scope = TestScope()
  private val analytics = mockk<Analytics>(relaxUnitFun = true)
  private val tracker = EmptyLibraryTracker(
    analytics = analytics,
    audiobookFolders = mockk<AudiobookFolders> {
      every { all() } returns flowOf(mapOf(FolderType.Root to listOf(mockk<DocumentFileWithUri>())))
      every { withoutAccess() } returns flowOf(mapOf(FolderType.SingleFolder to listOf(mockk<Uri>(), mockk<Uri>())))
    },
    bookContentRepo = mockk<BookContentRepo> {
      coEvery { all() } returns listOf(book().content.copy(isActive = false), book().content)
    },
    dispatcherProvider = DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
  )

  @Test
  fun `tells what is behind an empty library once`() = scope.runTest {
    tracker.track()
    tracker.track()

    verify(exactly = 1) {
      analytics.event(
        name = "library_empty",
        params = mapOf("folders" to "1", "folders_without_access" to "2", "inactive_books" to "1"),
      )
    }
  }
}
