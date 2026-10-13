plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.sleeptimer.api)
  api(projects.core.data.api)
  api(projects.core.common)
  api(projects.core.playback)
  api(projects.core.initializer)
  implementation(projects.core.analytics.api)

  implementation(libs.androidxCore)
  implementation(libs.androidxInterpolator)
}
