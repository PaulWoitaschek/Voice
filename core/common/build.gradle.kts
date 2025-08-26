plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

android {
  androidResources {
    enable = true
  }
}

dependencies {
  implementation(projects.core.strings)
  implementation(libs.appCompat)
  implementation(libs.material)
  api(libs.immutable)
  api(libs.datastore)
  implementation(libs.androidxCore)
  api(libs.navigation3.runtime)
  implementation(libs.serialization.json)

  testImplementation(kotlin("reflect"))
  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.robolectric)
}
