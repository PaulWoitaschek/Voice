plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.common)
  api(projects.navigation)
  implementation(projects.core.strings)
  api(projects.core.featureflag)
  api(projects.core.ui)
  api(projects.core.remoteconfig.api)
  api(projects.core.data.api)
  implementation(projects.core.documentfile)
  api(projects.core.audiobookshelf)
}
