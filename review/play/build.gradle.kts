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
    androidResources = true
  }
}

dependencies {
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.common)
  implementation(projects.datastore)
  api(libs.review)
  implementation(libs.lottie)
  implementation(libs.dagger.core)
}
