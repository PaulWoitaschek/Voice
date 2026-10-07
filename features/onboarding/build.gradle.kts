plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.navigation)
  implementation(projects.core.common)
  implementation(projects.core.strings)
  implementation(projects.core.ui)

  implementation(libs.coil)
  implementation(libs.androidxCore)
}
