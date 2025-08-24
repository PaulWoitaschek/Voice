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
  api(libs.review)

  implementation(libs.lottie)
  implementation(projects.common)
  implementation(projects.data)
  implementation(projects.datastore)
  implementation(projects.playback)
  implementation(projects.remoteconfig.core)
  implementation(projects.strings)
}
