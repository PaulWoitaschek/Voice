plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)

  implementation(libs.androidxCore)
  implementation(libs.materialDialog.core)
  implementation(libs.androidxCore)
  implementation(libs.material)
}
