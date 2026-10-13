plugins {
  id("voice.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

android {
  androidResources {
    enable = true
  }
}

dependencies {
  implementation(projects.core.common)
  implementation(projects.core.strings)
  api(projects.core.featureflag)
  api(projects.core.sleeptimer.api)
  api(projects.core.data.api)
  api(projects.core.analytics.api)

  implementation(libs.androidxCore)
  implementation(libs.coroutines.guava)
  api(libs.serialization.json)

  api(libs.media3.exoplayer)
  implementation(libs.media3.session)

  testImplementation(libs.media3.testUtils.core)
  testImplementation(libs.media3.testUtils.robolectric)
}
