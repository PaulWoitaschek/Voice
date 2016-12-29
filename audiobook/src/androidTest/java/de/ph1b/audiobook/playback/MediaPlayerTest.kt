package de.ph1b.audiobook.playback

import android.content.Context
import android.net.Uri
import android.os.PowerManager
import android.support.test.InstrumentationRegistry
import android.support.test.rule.ActivityTestRule
import com.google.android.exoplayer2.DefaultLoadControl
import com.google.android.exoplayer2.ExoPlayerFactory
import com.google.android.exoplayer2.SimpleExoPlayer
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.source.ExtractorMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import de.ph1b.audiobook.features.MainActivity
import de.ph1b.audiobook.features.bookPlaying.Equalizer
import de.ph1b.audiobook.injection.App
import de.ph1b.audiobook.playback.PlayStateManager.PlayState
import org.junit.Before
import org.junit.Rule
import org.junit.Test


/**
 * TODO
 *
 * @author Paul Woitaschek
 */
class MediaPlayerTest {

  lateinit var player: MediaPlayer
  lateinit var playStateManager: PlayStateManager
  lateinit var exoPlayer: SimpleExoPlayer

  @get:Rule val activityRule = ActivityTestRule(MainActivity::class.java)

  @Before fun setup() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      val context = activityRule.activity
      val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
      exoPlayer = ExoPlayerFactory.newSimpleInstance(context, DefaultTrackSelector(), DefaultLoadControl())
      val prefsManager = App.component.prefsManager
      playStateManager = PlayStateManager()
      val dataSourceFactory = DefaultDataSourceFactory(context, "exoTest")
      val equalizer = Equalizer(context)
      val wakeLockManager = WakeLockManager(powerManager)
      player = MediaPlayer(exoPlayer, dataSourceFactory, playStateManager, equalizer, wakeLockManager, prefsManager)
    }
  }

  @Test fun blah() {
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
      check(playStateManager.playState.value == PlayState.STOPPED)
      println("NOW")
      val one = Uri.parse("file:///android_asset/one.m4a")
      exoPlayer.prepare(ExtractorMediaSource(one, DefaultDataSourceFactory(activityRule.activity, "blah"), DefaultExtractorsFactory(), null, null))
      exoPlayer.playWhenReady = true
    }

    Thread.sleep(5000)
  }

}