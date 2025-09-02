plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.sleeptimer.api)
  implementation(projects.core.data.api)
  implementation(projects.core.common)
  implementation(projects.core.playback)
  implementation(projects.core.initializer)
  implementation(projects.core.logging.core)

  implementation(libs.datastore)
  implementation(libs.androidxCore)
  implementation(libs.seismic)
  implementation(libs.appCompat)

  testImplementation(libs.junit)
  testImplementation(libs.mockk)
  testImplementation(libs.koTest.assert)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.robolectric)
}
