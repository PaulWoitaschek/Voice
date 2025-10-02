plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

android {
  androidResources {
    enable = true
  }
}

dependencies {
  implementation(projects.core.strings)
  implementation(projects.core.common)
  implementation(projects.core.ui)
  implementation(projects.core.initializer)
  implementation(projects.core.data.api)
  implementation(projects.core.playback)

  implementation(libs.appCompat)
  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.androidxCore)

  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.robolectric)
}
