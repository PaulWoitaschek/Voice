plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.navigation)
  implementation(projects.core.common)
  implementation(projects.core.datastore)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  implementation(projects.core.analytics.api)

  implementation(libs.coil)
  implementation(libs.androidxCore)
}
