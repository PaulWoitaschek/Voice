plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.analytics.api)
  api(projects.core.common)
  api(projects.core.data.api)
  api(projects.core.playback)
  api(projects.core.featureflag)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
}
