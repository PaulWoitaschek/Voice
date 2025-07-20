plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

metro {
  interop {
    includeDagger()
  }
}

dependencies {
  implementation(projects.common)
  implementation(projects.search)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.sleepTimer)
  implementation(projects.scanner)

  implementation(libs.lifecycle)
  implementation(libs.documentFile)
  implementation(libs.dagger.core)
  implementation(libs.datastore)

  testImplementation(libs.bundles.testing.jvm)
}
