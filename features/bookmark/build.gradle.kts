plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.common)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  implementation(projects.core.playback)
  implementation(projects.navigation)
  implementation(projects.core.data.api)

  implementation(libs.materialDialog.core)
  implementation(libs.materialDialog.input)
}
