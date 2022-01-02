plugins {
  id("voice-android-library")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.data)

  implementation(libs.timber)
  implementation(libs.coroutines.core)
  implementation(libs.media)
  implementation(libs.picasso)
  implementation(libs.androidxCore)
  implementation(libs.prefs.core)

  implementation(libs.dagger.core)

  implementation(libs.exoPlayer.core)
  implementation(libs.exoPlayer.flac) { isTransitive = false }
}
