plugins {
  id("voice.library")
  id("voice.compose")
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
  implementation(projects.sleepTimer)

  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.androidxCore)
  implementation(libs.prefs.core)
  implementation(libs.materialDialog.core)
  implementation(libs.androidxCore)
  implementation(libs.material)

  implementation(libs.dagger.core)
}
