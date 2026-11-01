package voice.core.playback.receiver

import android.app.Application
import android.content.Intent
import android.os.Looper
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.verify
import java.time.Instant
import kotlin.test.BeforeTest
import kotlin.test.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import voice.core.common.rootGraph
import voice.core.data.BookId
import voice.core.data.Chapter
import voice.core.data.ChapterId
import voice.core.data.ListeningEvent
import voice.core.data.repo.BookRepository
import voice.core.data.store.CurrentBookStore
import voice.core.playback.MemoryDataStore
import voice.core.playback.PlayerControllerImpl
import voice.core.playback.session.search.book

@RunWith(AndroidJUnit4::class)
class WidgetButtonReceiverTest {

  private val context = ApplicationProvider.getApplicationContext<Application>()
  private val intents = WidgetButtonIntentsImpl(context)
  private val book =
    book(
      chapters =
        listOf(
          Chapter(
            id = ChapterId("chapter"),
            name = "Chapter",
            duration = 10_000,
            fileLastModified = Instant.EPOCH,
            markData = emptyList(),
            fileSize = 0,
          ),
        ),
    )
  private var currentBookId: BookId? = book.id
  private val player =
    mockk<PlayerControllerImpl>(relaxUnitFun = true) {
      coEvery { awaitConnect() } returns null
    }
  private val bookRepository =
    mockk<BookRepository> {
      coEvery { get(book.id) } returns book
    }

  @BeforeTest
  fun setUp() {
    rootGraph =
      object : WidgetButtonReceiver.Graph {
        override fun inject(target: WidgetButtonReceiver) {
          target.player = player
          target.bookRepository = bookRepository
          target.currentBookStore = CurrentBookStore(MemoryDataStore(currentBookId))
        }
      }
  }

  @Test
  fun `start over is recorded as coming from the widget`() {
    send(intents.startOver())

    verify {
      player.setPosition(
        0,
        book.chapters.first().id,
        ListeningEvent.Type.Seek,
        ListeningEvent.Source.Widget,
      )
    }
  }

  @Test
  fun `the book paused for another one is recorded as paused from the widget`() {
    currentBookId = BookId("other")

    send(intents.playBook(book.id))

    verify { player.pauseIfCurrentBookDifferentFrom(book.id, ListeningEvent.Source.Widget) }
  }

  private fun send(intent: Intent) {
    context.sendBroadcast(intent)
    shadowOf(Looper.getMainLooper()).idle()
  }
}
