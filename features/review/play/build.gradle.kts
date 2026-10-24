plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  implementation(libs.coil)

  implementation(projects.core.analytics.api)
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.playback)
  implementation(projects.core.featureflag)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
}
