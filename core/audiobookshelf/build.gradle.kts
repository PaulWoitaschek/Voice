plugins {
  id("voice.library")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

android {
  androidResources {
    enable = true
  }
}

dependencies {
  api(projects.core.analytics.api)
  implementation(projects.core.common)
  api(projects.core.data.api)
  api(projects.core.initializer)
  api(projects.core.playback)
  implementation(projects.core.strings)

  implementation(libs.androidxCore)
  implementation(libs.retrofit.core)
  implementation(libs.retrofit.serialization)
  implementation(libs.okhttp)
  implementation(libs.serialization.json)
  implementation(libs.media3.exoplayer)
  api(libs.media3.datasource.okhttp)

  testImplementation(libs.okhttp.mockwebserver)
}
