plugins {
  id("voice.library")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.initializer)
  implementation(projects.core.playback)
  implementation(projects.core.scanner)

  implementation(libs.androidxCore)
  implementation(libs.datastore)
  implementation(libs.serialization.json)
}
