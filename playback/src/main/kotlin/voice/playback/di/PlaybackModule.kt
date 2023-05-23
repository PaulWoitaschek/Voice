package voice.playback.di

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.MediaSource
import androidx.media3.exoplayer.source.ProgressiveMediaSource
import androidx.media3.extractor.DefaultExtractorsFactory
import androidx.media3.session.MediaLibraryService
import com.squareup.anvil.annotations.ContributesTo
import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import voice.playback.misc.VolumeGain
import voice.playback.notification.MainActivityIntentProvider
import voice.playback.player.DurationInconsistenciesUpdater
import voice.playback.player.OnlyAudioRenderersFactory
import voice.playback.player.VoicePlayer
import voice.playback.player.onAudioSessionIdChanged
import voice.playback.playstate.PlayStateDelegatingListener
import voice.playback.playstate.PositionUpdater
import voice.playback.session.LibrarySessionCallback
import voice.playback.session.PlaybackService
import voice.playback.session.SleepTimer
import voice.playback.session.SleepTimerCommandUpdater
import kotlin.time.Duration

@Module
@ContributesTo(PlaybackScope::class)
object PlaybackModule {

  @Provides
  @PlaybackScope
  fun mediaSourceFactory(context: Context): MediaSource.Factory {
    val dataSourceFactory = DefaultDataSource.Factory(context)
    val extractorsFactory = DefaultExtractorsFactory()
      .setConstantBitrateSeekingEnabled(true)
    return ProgressiveMediaSource.Factory(dataSourceFactory, extractorsFactory)
  }

  @Provides
  @PlaybackScope
  fun player(
    context: Context,
    onlyAudioRenderersFactory: OnlyAudioRenderersFactory,
    mediaSourceFactory: MediaSource.Factory,
    playStateDelegatingListener: PlayStateDelegatingListener,
    positionUpdater: PositionUpdater,
    volumeGain: VolumeGain,
    durationInconsistenciesUpdater: DurationInconsistenciesUpdater,
  ): Player {
    val audioAttributes = AudioAttributes.Builder()
      .setContentType(C.AUDIO_CONTENT_TYPE_SPEECH)
      .setUsage(C.USAGE_MEDIA)
      .build()
    return ExoPlayer.Builder(context, onlyAudioRenderersFactory, mediaSourceFactory)
      .setAudioAttributes(audioAttributes, true)
      .setHandleAudioBecomingNoisy(true)
      .setWakeMode(C.WAKE_MODE_LOCAL)
      .build()
      .also { player ->
        playStateDelegatingListener.attachTo(player)
        positionUpdater.attachTo(player)
        durationInconsistenciesUpdater.attachTo(player)
        player.onAudioSessionIdChanged {
          volumeGain.audioSessionId = it
        }
      }
  }

  @Provides
  @PlaybackScope
  fun scope(): CoroutineScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  @Provides
  @PlaybackScope
  fun session(
    service: PlaybackService,
    player: VoicePlayer,
    callback: LibrarySessionCallback,
    mainActivityIntentProvider: MainActivityIntentProvider,
    scope: CoroutineScope,
    sleepTimer: SleepTimer,
    sleepTimerCommandUpdater: SleepTimerCommandUpdater,
  ): MediaLibraryService.MediaLibrarySession {
    return MediaLibraryService.MediaLibrarySession.Builder(service, player, callback)
      .setSessionActivity(mainActivityIntentProvider.toCurrentBook())
      .build()
      .also { session ->
        scope.launch {
          sleepTimer.leftSleepTimeFlow
            .map { it != Duration.ZERO }
            .distinctUntilChanged()
            .collect { sleepTimerActive ->
              sleepTimerCommandUpdater.update(session, sleepTimerActive)
            }
        }
      }
  }
}
