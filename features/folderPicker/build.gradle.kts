plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.ui)
  api(projects.core.common)
  implementation(projects.core.strings)
  api(projects.core.data.api)
  api(projects.core.documentfile)
  api(projects.core.scanner)
  api(projects.navigation)
  api(projects.core.featureflag)
  api(projects.core.audiobookshelf)

  api(libs.documentFile)
}
