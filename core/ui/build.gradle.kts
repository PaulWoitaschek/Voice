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
  api(libs.datastore)
  implementation(libs.appCompat)
  api(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.initializer)
  implementation(projects.core.strings)
  implementation(libs.lifecycle.compose)
  implementation(libs.materialKolor)
  implementation(libs.coil)
  implementation(libs.navigation3.ui)
}
