plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.analytics.api)
  implementation(projects.core.search)
  implementation(projects.core.playback.api)
  implementation(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.scanner)
  implementation(projects.core.featureflag)
  implementation(projects.core.kioskmode)
  implementation(projects.core.audiobookshelf.api)

  implementation(libs.lifecycle)
  implementation(libs.lifecycle.compose)
  implementation(libs.documentFile)
  implementation(libs.navigation3.ui)
}
