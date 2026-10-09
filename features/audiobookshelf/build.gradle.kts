plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.audiobookshelf)
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  implementation(projects.navigation)

  implementation(libs.androidxCore)
  implementation(libs.coil)
  implementation(libs.navigation3.ui)

  testImplementation(libs.molecule)
}
