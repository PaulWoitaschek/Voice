plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

android {
  androidResources.enable = true
}

dependencies {
  api(libs.datastore)
  implementation(libs.appCompat)
  api(projects.core.data.api)
  implementation(projects.core.initializer)
  implementation(projects.core.strings)
  implementation(libs.lifecycle.compose)
  implementation(libs.materialKolor)
}
