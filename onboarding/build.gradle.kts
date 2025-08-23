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
  implementation(projects.common)
  implementation(projects.strings)

  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.androidxCore)
}
