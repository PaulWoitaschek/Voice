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
  implementation(projects.core.analytics.api)
  implementation(projects.core.common)
  implementation(projects.core.data.api)
  implementation(projects.core.initializer)
  implementation(projects.core.playback)
  implementation(projects.core.strings)

  implementation(libs.androidxCore)
  implementation(libs.bundles.retrofit)
  implementation(libs.okhttp)
  implementation(libs.serialization.json)
  implementation(libs.media3.exoplayer)
  implementation(libs.media3.datasource.okhttp)

  testImplementation(libs.bundles.testing.jvm)
  testImplementation(libs.okhttp.mockwebserver)
}
