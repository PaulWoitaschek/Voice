plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.initializer)
  implementation(projects.core.playback.api)
  implementation(projects.core.scanner)

  implementation(libs.datastore)
}
