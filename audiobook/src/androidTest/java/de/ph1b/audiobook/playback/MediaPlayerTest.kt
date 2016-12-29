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


/**
 * TODO
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
    val testObserver = TestObserver<PlayState>()
    playStateManager.playStateStream().subscribe(testObserver)

    val chapters = files.map {
      val result = MediaAnalyzer.compute(it)
      Chapter(it, result.chapterName, result.duration)
    }
    val book = Book(5, Book.Type.COLLECTION_FILE, "author", files.first(), 0, "bookName", chapters, 1.0F, "root")
    player.init(book)
    player.play()

    Thread.sleep(10000)
    assertThat(testObserver.values()).containsExactly(PlayState.STOPPED, PlayState.PLAYING, PlayState.STOPPED)
  }
}