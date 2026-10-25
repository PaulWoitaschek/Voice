plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

android {
  androidResources.enable = true
}

dependencies {
  implementation(libs.appCompat)
  api(projects.core.data.api)
  api(projects.core.datastore)
  implementation(projects.core.initializer)
  implementation(projects.core.strings)
  implementation(libs.lifecycle.compose)
  implementation(libs.materialKolor)
  implementation(libs.coil)
  implementation(libs.navigation3.ui)

  testImplementation(libs.compose.ui.testJunit)
}
