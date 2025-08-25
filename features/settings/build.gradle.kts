plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.common)
  implementation(projects.navigation)
  implementation(projects.core.strings)
  implementation(projects.core.playback)

  implementation(libs.androidxCore)
  implementation(libs.materialDialog.core)
  implementation(libs.androidxCore)
  implementation(libs.material)
}
