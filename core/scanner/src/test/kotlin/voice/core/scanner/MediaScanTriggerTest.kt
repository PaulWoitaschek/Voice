package voice.core.scanner

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import kotlin.test.Test
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import voice.core.common.DispatcherProvider

class MediaScanTriggerTest {

  private val scope = TestScope()
  private val scanner = mockk<MediaScanner>()
  private val coverScanner = mockk<CoverScanner>()
  private val trigger =
    MediaScanTrigger(
      audiobookFolders = mockk { every { all() } returns flowOf(emptyMap()) },
      scanner = scanner,
      coverScanner = coverScanner,
      bookRepo = mockk { coEvery { all() } returns emptyList() },
      documentFileFactory = mockk(),
      dispatcherProvider =
        DispatcherProvider(scope.coroutineContext, scope.coroutineContext, scope.coroutineContext),
    )

  @Test
  fun `a request while covers are looked for scans again`() = scope.runTest {
    val coversDone = CompletableDeferred<Unit>()
    coEvery { scanner.scan(any()) } just Runs
    coEvery { coverScanner.scan(any()) } coAnswers { coversDone.await() }

    trigger.scan()
    runCurrent()
    trigger.scan()
    runCurrent()

    coVerify(exactly = 2) { scanner.scan(any()) }
    coversDone.complete(Unit)
  }

  @Test
  fun `a request while books are looked for does not scan again`() = scope.runTest {
    val booksDone = CompletableDeferred<Unit>()
    coEvery { scanner.scan(any()) } coAnswers { booksDone.await() }
    coEvery { coverScanner.scan(any()) } just Runs

    trigger.scan()
    runCurrent()
    trigger.scan()
    booksDone.complete(Unit)
    runCurrent()

    coVerify(exactly = 1) { scanner.scan(any()) }
  }
}
