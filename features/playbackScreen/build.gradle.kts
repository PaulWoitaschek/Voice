plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.navigation)
  api(projects.core.common)
  implementation(projects.core.strings)
  api(projects.core.playback)
  api(projects.core.audiobookshelf)
  api(projects.core.data.api)
  api(projects.core.featureflag)
  api(projects.core.ui)
  api(projects.core.sleeptimer.api)

  testImplementation(libs.compose.ui.testJunit)
}
