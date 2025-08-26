plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

android {
  androidResources.enable = true
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.strings)
  implementation(libs.lifecycle.viewmodel.compose)
}
