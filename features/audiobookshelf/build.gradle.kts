plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.audiobookshelf)
  api(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  api(projects.navigation)
}
