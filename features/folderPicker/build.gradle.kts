plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.ui)
  implementation(projects.core.common)
  implementation(projects.core.strings)
  implementation(projects.core.data.api)
  implementation(projects.core.documentfile)
  implementation(projects.core.scanner)
  implementation(projects.navigation)
  implementation(projects.core.featureflag)
  implementation(projects.core.kioskmode)
  implementation(projects.core.analytics.api)
  implementation(projects.core.audiobookshelf)

  implementation(libs.coil)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)
}
