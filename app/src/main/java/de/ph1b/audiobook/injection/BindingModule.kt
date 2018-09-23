package de.ph1b.audiobook.injection

import dagger.Module
import dagger.android.ContributesAndroidInjector
import de.ph1b.audiobook.playback.PlaybackService

/**
 * Module for dagger bindings
 */
@Module
abstract class BindingModule {

  @ContributesAndroidInjector(modules = [PlaybackModule::class])
  @PerService
  abstract fun playbackService(): PlaybackService
}
