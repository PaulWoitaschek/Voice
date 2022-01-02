plugins {
  id("voice-android-library")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

android {
  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)

  implementation(libs.timber)
  implementation(libs.coroutines.core)
  implementation(libs.picasso)
  implementation(libs.androidxCore)
  implementation(libs.prefs.core)
  implementation(libs.materialDialog.core)
  implementation(libs.androidxCore)
  implementation(libs.constraintLayout)
  implementation(libs.material)

  implementation(libs.dagger.core)
}
