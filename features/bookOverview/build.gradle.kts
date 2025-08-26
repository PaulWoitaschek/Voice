plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.navigation)
  implementation(projects.core.common)
  implementation(projects.core.search)
  implementation(projects.core.ui)
  implementation(projects.core.strings)
  implementation(projects.core.playback)
  implementation(projects.core.data.api)
  implementation(projects.core.scanner)

  implementation(libs.lifecycle)
  implementation(libs.documentFile)
  implementation(libs.datastore)

  testImplementation(libs.bundles.testing.jvm)
}
