plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

android {
  androidResources {
    enable = true
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
  implementation(projects.remoteconfig.core)
}
