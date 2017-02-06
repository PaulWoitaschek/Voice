package de.ph1b.audiobook.playback

import android.support.test.runner.AndroidJUnit4
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.MediaAnalyzer
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.TimeUnit


/**
 * Tests for the media player
 *
 * @author Paul Woitaschek
 */
@RunWith(value = AndroidJUnit4::class)
class MediaPlayerTest {

  private lateinit var player: MediaPlayer
  private lateinit var playStateManager: PlayStateManager
  private lateinit var book: Book


  @Before fun setup() {
    onMainThreadSync {
      val chapters = prepareTestFiles().map {
        val result = MediaAnalyzer.compute(it)
        Chapter(it, result.chapterName, result.duration)
      }
      book = Book(5, Book.Type.COLLECTION_FILE, "author", chapters.first().file, 0, "bookName", chapters, 1.0F, "root")

      playStateManager = App.component.playStateManager
      player = App.component.player
    }
  }

  @Test fun testPlaybackCycle() {
    player.stop()
    waitFor(PlayState.STOPPED)

    val regularCompletionObserver = playStateManager.playStateStream().test()

    player.init(book)
    player.play()

    Thread.sleep(10000)

    // check that we are in stopped again after the book ended
    assertThat(regularCompletionObserver.values()).containsExactly(PlayState.STOPPED, PlayState.PAUSED, PlayState.PLAYING, PlayState.STOPPED)

    // check that we are in the last chapter
    assertThat(player.book!!.chapters.last()).isEqualTo(player.book!!.currentChapter())

    // after we ended and click play again assert that we are playing
    player.play()
    assertThat(playStateManager.playState).isEqualTo(PlayState.PLAYING)

    // make sure we are in the first chapter again
    assertThat(player.book!!.chapters.first()).isEqualTo(player.book!!.currentChapter())
  }

  fun waitFor(playState: PlayState) {
    // wait for max 10 secs to get to stopped state again
    playStateManager.playStateStream()
      .filter { it == playState }
      .timeout(10, TimeUnit.SECONDS)
      .blockingFirst()
  }
}