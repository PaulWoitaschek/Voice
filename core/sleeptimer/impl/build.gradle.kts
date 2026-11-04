plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.data.api)
  implementation(projects.core.datastore)
  implementation(projects.core.common)
  implementation(projects.core.playback.api)
  implementation(projects.core.initializer)
  implementation(projects.core.analytics.api)

  implementation(libs.appCompat)
}
