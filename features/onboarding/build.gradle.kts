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
  implementation(projects.navigation)
  implementation(projects.core.common)
  implementation(projects.core.strings)

  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.androidxCore)
}
