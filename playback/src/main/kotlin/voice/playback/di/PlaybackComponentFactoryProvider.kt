package voice.playback.di

interface PlaybackComponentFactoryProvider {

  fun factory(): PlaybackComponent.Factory
}
