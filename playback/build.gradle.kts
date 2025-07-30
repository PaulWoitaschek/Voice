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

metro {
  // necessary as SleepTimer is scoped to AppScope and used in the PlaybackScope.
  // without this, compilation will fail.
  enableScopedInjectClassHints.set(true)
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.data)

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
