plugins {
  id("voice-android-library")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

android {
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.sleepTimer)

  implementation(libs.timber)
  implementation(libs.datastore)
  implementation(libs.coroutines.core)
  implementation(libs.picasso)
  implementation(libs.coil)
  implementation(libs.prefs.core)
  implementation(libs.materialDialog.core)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)

  implementation(libs.dagger.core)

  implementation(libs.bundles.compose)
}
