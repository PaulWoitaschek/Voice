plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.navigation)
  api(projects.core.common)
  api(projects.core.search)
  api(projects.core.ui)
  implementation(projects.core.strings)
  api(projects.core.playback)
  api(projects.core.data.api)
  api(projects.core.scanner)
  api(projects.core.featureflag)
  api(projects.core.audiobookshelf)

  implementation(libs.documentFile)
  implementation(libs.navigation3.ui)
}
