package voice.core.audiobookshelf.download

import android.content.Context
import android.os.Looper
import androidx.core.net.toUri
import androidx.media3.exoplayer.offline.DefaultDownloadIndex
import androidx.media3.exoplayer.offline.Download
import androidx.media3.exoplayer.offline.DownloadRequest
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okhttp3.OkHttpClient
import org.junit.After
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.audiobookshelf.MemoryBookRepository
import voice.core.audiobookshelf.MemoryDataStore
import voice.core.audiobookshelf.account.Account
import voice.core.audiobookshelf.account.AccountStore
import voice.core.audiobookshelf.http.AudiobookshelfHttp
import voice.core.audiobookshelf.sync.chapters
import voice.core.audiobookshelf.sync.item
import voice.core.audiobookshelf.sync.toBookContent
import voice.core.audiobookshelf.sync.track
import voice.core.data.Book
import voice.core.data.BookId
import voice.core.data.CustomMetadata

@RunWith(AndroidJUnit4::class)
class AudiobookshelfDownloadsTest {

  private val context = ApplicationProvider.getApplicationContext<Context>()
  private val accountStore = AccountStore(MemoryDataStore<Account?>(null))
  private val media =
    AudiobookshelfMedia(context, AudiobookshelfHttp(accountStore, OkHttpClient()), accountStore)
  private val book = book()
  private val covers = mockk<DownloadedCovers>(relaxUnitFun = true)
  private val downloads =
    AudiobookshelfDownloads(
      context = context,
      media = media,
      bookRepository = MemoryBookRepository(book),
      preferencesStore = mockk(relaxed = true),
      notifications = mockk(relaxed = true),
      covers = covers,
    )

  @After
  fun tearDown() {
    media.cache.release()
  }

  @Test
  fun `a sync before the stored downloads are read keeps the covers of downloaded books`() =
    runTest {
      backgroundScope.launch { downloads.reconcile() }
      repeat(3) {
        runCurrent()
        shadowOf(Looper.getMainLooper()).idle()
      }

      coVerify(exactly = 0) { covers.sync(any(), any()) }
    }

  @Test
  fun `the sync keeps the cover of a downloaded book`() = runTest {
    val chapter = book.chapters.single().id.value
    DefaultDownloadIndex(media.databaseProvider)
      .putDownload(
        Download(
          DownloadRequest.Builder(chapter, chapter.toUri())
            .setData(book.id.value.encodeToByteArray())
            .build(),
          Download.STATE_COMPLETED,
          0,
          0,
          1_000,
          Download.STOP_REASON_NONE,
          Download.FAILURE_REASON_NONE,
        ),
      )
    var synced: Pair<List<Book>, Set<BookId>>? = null
    coEvery { covers.sync(any(), any()) } answers { synced = firstArg<List<Book>>() to secondArg() }

    // it reads the stored downloads when it is created
    val _ = downloads.downloadManager
    backgroundScope.launch { downloads.reconcile() }
    until { synced != null }

    assertEquals(listOf(book) to setOf(book.id), synced)
  }

  @Test
  fun `downloading a book keeps its cover`() = runTest {
    var kept: Book? = null
    coEvery { covers.keep(any()) } answers { kept = firstArg() }

    backgroundScope.launch { downloads.download(book.id) }
    until { kept != null }

    assertEquals(book.id, kept?.id)
  }

  // the downloads run on the main thread and on threads of their own
  private fun TestScope.until(done: () -> Boolean) {
    repeat(500) {
      if (done()) return
      runCurrent()
      shadowOf(Looper.getMainLooper()).idle()
      Thread.sleep(10)
    }
  }

  private fun book(): Book {
    val item = item(tracks = listOf(track(1, 0.0, 60.0, "a.mp3")))
    val chapters = item.chapters()
    val content = item.toBookContent(chapters, existing = null)
    return Book(content, chapters, CustomMetadata(content.id))
  }
}
