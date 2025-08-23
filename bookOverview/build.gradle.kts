plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.common)
  implementation(projects.search)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.scanner)

  implementation(libs.lifecycle)
  implementation(libs.documentFile)
  implementation(libs.datastore)

  testImplementation(libs.bundles.testing.jvm)
}
