plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.common)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  api(projects.core.playback)
  api(projects.core.sleeptimer.api)
  api(projects.navigation)
  api(projects.core.data.api)
  api(projects.core.featureflag)
}
