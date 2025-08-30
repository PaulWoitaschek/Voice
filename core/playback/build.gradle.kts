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
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.data.api)

  implementation(libs.androidxCore)
  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.coroutines.guava)
  implementation(libs.serialization.json)

  implementation(libs.media3.exoplayer)
  implementation(libs.media3.session)

  testImplementation(libs.bundles.testing.jvm)
  testImplementation(libs.media3.testUtils.core)
  testImplementation(libs.media3.testUtils.robolectric)
}
