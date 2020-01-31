package de.ph1b.audiobook.playback.di

interface PlaybackComponentFactoryProvider {

  fun factory(): PlaybackComponent.Factory
}
