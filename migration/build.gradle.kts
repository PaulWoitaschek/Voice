plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data.api)
  implementation(projects.navigation)

  implementation(libs.datastore)

  testImplementation(libs.bundles.testing.jvm)
}
