package voice.core.audiobookshelf.download

import androidx.media3.exoplayer.offline.Download
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class BookDownloadStateTest {

  private val bookId = BookId("abs://item/1")
  private val first = chapter("abs://item/1/track/1/a", size = 100)
  private val second = chapter("abs://item/1/track/2/b", size = 300)

  @Test
  fun `a book without downloaded files has no download`() {
    assertNull(bookDownloadState(listOf(first, second), emptyMap(), waitingFor = null))
  }

  @Test
  fun `the progress counts the bytes of all files of the book`() {
    val files = mapOf(
      first.id.value to file(Download.STATE_COMPLETED, bytes = 100),
      second.id.value to file(Download.STATE_DOWNLOADING, bytes = 100),
    )

    val state = bookDownloadState(listOf(first, second), files, waitingFor = null)

    assertEquals(
      BookDownloadState.Downloading(progress = 0.5F, downloadedBytes = 200, totalBytes = 400, waitingFor = null),
      state,
    )
  }

  @Test
  fun `files that haven't started yet keep the progress honest`() {
    val files = mapOf(first.id.value to file(Download.STATE_DOWNLOADING, bytes = 100))

    val state = bookDownloadState(listOf(first, second), files, waitingFor = null) as BookDownloadState.Downloading

    assertEquals(0.25F, state.progress)
  }

  @Test
  fun `without the sizes the progress counts the finished files`() {
    val unknown = listOf(first.copy(fileSize = 0), second.copy(fileSize = 0))
    val files = mapOf(
      first.id.value to file(Download.STATE_COMPLETED, bytes = 100),
      second.id.value to file(Download.STATE_DOWNLOADING, bytes = 100),
    )

    val state = bookDownloadState(unknown, files, waitingFor = null) as BookDownloadState.Downloading

    assertEquals(0.5F, state.progress)
    assertEquals(0L, state.totalBytes)
  }

  @Test
  fun `a book is downloaded once all its files are`() {
    val files = mapOf(
      first.id.value to file(Download.STATE_COMPLETED, bytes = 100),
      second.id.value to file(Download.STATE_COMPLETED, bytes = 300),
    )

    assertEquals(BookDownloadState.Downloaded(bytes = 400), bookDownloadState(listOf(first, second), files, waitingFor = null))
  }

  @Test
  fun `a new file on the server makes a downloaded book download again`() {
    val files = mapOf(first.id.value to file(Download.STATE_COMPLETED, bytes = 100))

    val state = bookDownloadState(listOf(first, second), files, waitingFor = null)

    assertEquals(BookDownloadState.Downloading(0.25F, 100, 400, waitingFor = null), state)
  }

  @Test
  fun `a failed file fails the book`() {
    val files = mapOf(
      first.id.value to file(Download.STATE_COMPLETED, bytes = 100),
      second.id.value to file(Download.STATE_FAILED, bytes = 10),
    )

    assertEquals(BookDownloadState.Failed, bookDownloadState(listOf(first, second), files, waitingFor = null))
  }

  @Test
  fun `a failed file only fails the book once the other files are done`() {
    val files = mapOf(
      first.id.value to file(Download.STATE_FAILED, bytes = 10),
      second.id.value to file(Download.STATE_DOWNLOADING, bytes = 100),
    )

    assertIs<BookDownloadState.Downloading>(bookDownloadState(listOf(first, second), files, waitingFor = null))
  }

  @Test
  fun `a stopped download is gone right away`() {
    val files = mapOf(
      first.id.value to file(Download.STATE_REMOVING, bytes = 100),
      second.id.value to file(Download.STATE_REMOVING, bytes = 0),
    )

    assertNull(bookDownloadState(listOf(first, second), files, waitingFor = null))
  }

  @Test
  fun `a waiting download tells what it waits for`() {
    val files = mapOf(first.id.value to file(Download.STATE_QUEUED, bytes = 0))

    val state = bookDownloadState(listOf(first, second), files, waitingFor = WaitingFor.Wifi) as BookDownloadState.Downloading

    assertEquals(WaitingFor.Wifi, state.waitingFor)
  }

  private fun file(
    state: Int,
    bytes: Long,
  ) = FileDownload(bookId = bookId, state = state, bytesDownloaded = bytes)

  private fun chapter(
    id: String,
    size: Long,
  ) = Chapter(
    id = ChapterId(id),
    name = null,
    duration = 60_000,
    fileLastModified = Instant.EPOCH,
    fileSize = size,
    markData = emptyList(),
  )
}
