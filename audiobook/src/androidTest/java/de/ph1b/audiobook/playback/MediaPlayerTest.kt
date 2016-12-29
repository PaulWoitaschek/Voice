package de.ph1b.audiobook.playback

import android.content.Context
import android.os.PowerManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import de.ph1b.audiobook.Book
import de.ph1b.audiobook.Chapter
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.features.bookPlaying.Equalizer
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.misc.MediaAnalyzer
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import io.reactivex.observers.TestObserver
import org.assertj.core.api.Assertions.assertThat
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.io.File
import java.util.*
import java.util.concurrent.TimeUnit


/**
 * Tests for the media player
 *
 * @author Paul Woitaschek
 */
class MediaPlayerTest {

  lateinit var player: MediaPlayer
  lateinit var playStateManager: PlayStateManager

  val files = ArrayList<File>()

  @get:Rule val activityRule = ActivityTestRule(MainActivity::class.java)

  /** copy files to the internal storage */
  private fun initFiles() {
    val instrumentationContext = InstrumentationRegistry.getContext()
    val testFolder = File(InstrumentationRegistry.getTargetContext().filesDir, "testFolder")
    testFolder.mkdirs()
    instrumentationContext.assets.list("samples").forEach { asset ->
      val out = File(testFolder, asset)
      out.outputStream().use { outputStream ->
        instrumentationContext.assets.open("samples/$asset").use { inputStream ->
          inputStream.copyTo(outputStream)
        }
      }
      files.add(out)
    }
  }

  @Before fun setup() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      val context = activityRule.activity

      initFiles()

      val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
      val exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector(), DefaultLoadControl())
      val prefsManager = App.component.prefsManager
      playStateManager = PlayStateManager()
      val dataSourceFactory = DefaultDataSourceFactory(context, "exoTest")
      val equalizer = Equalizer(context)
      val wakeLockManager = WakeLockManager(powerManager)
      player = MediaPlayer(exoPlayer, dataSourceFactory, playStateManager, equalizer, wakeLockManager, prefsManager)
    }
  }

  @Test fun testPlaybackCycle() {
    val regularCompletionObserver = TestObserver<PlayState>()
    playStateManager.playStateStream().subscribe(regularCompletionObserver)

    val chapters = files.map {
      val result = MediaAnalyzer.compute(it)
      Chapter(it, result.chapterName, result.duration)
    }
    val book = Book(5, Book.Type.COLLECTION_FILE, "author", files.first(), 0, "bookName", chapters, 1.0F, "root")
    player.init(book)
    player.play()

    waitFor(PlayState.STOPPED)

    // check that we are in stopped again after the book ended
    assertThat(regularCompletionObserver.values()).containsExactly(PlayState.STOPPED, PlayState.PLAYING, PlayState.STOPPED)

    // check that we are in the last chapter
    assertThat(player.book()!!.chapters.last()).isEqualTo(player.book()!!.currentChapter())

    // after we ended and click play again assert that we are playing
    player.play()
    assertThat(playStateManager.playState).isEqualTo(PlayState.PLAYING)

    // make sure we are in the first chapter again
    assertThat(player.book()!!.chapters.first()).isEqualTo(player.book()!!.currentChapter())
  }

  fun waitFor(playState: PlayState) {
    // wait for max 10 secs to get to stopped state again
    playStateManager.playStateStream()
      .filter { it == playState }
      .timeout(10, TimeUnit.SECONDS)
      .blockingFirst()
  }
}