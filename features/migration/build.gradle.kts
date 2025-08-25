plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.common)
  implementation(projects.core.strings)
  implementation(projects.core.playback)
  implementation(projects.core.data.api)
  implementation(projects.navigation)

  implementation(libs.datastore)

  testImplementation(libs.bundles.testing.jvm)
}
