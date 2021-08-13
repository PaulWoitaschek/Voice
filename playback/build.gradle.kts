plugins {
  id("com.android.library")
  id("kotlin-android")
  id("com.squareup.anvil")
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.data)
  implementation(projects.prefs)

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
