plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.common)
  implementation(projects.core.playback)
  implementation(projects.core.initializer)
  implementation(projects.core.logging.api)
  implementation(projects.core.analytics.api)

  implementation(libs.androidxCore)
  implementation(libs.appCompat)
}
