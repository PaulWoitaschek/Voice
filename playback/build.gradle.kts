plugins {
  id("com.android.library")
  id("kotlin-android")
  id("com.squareup.anvil")
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(project(":common"))
  implementation(project(":strings"))
  implementation(project(":data"))
  implementation(project(":prefs"))

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
