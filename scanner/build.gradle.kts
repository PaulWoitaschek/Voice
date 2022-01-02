plugins {
  id("voice-android-library")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
  generateDaggerFactoriesOnly.set(true)
}

dependencies {
  implementation(projects.ffmpeg)
  implementation(projects.data)
  implementation(projects.common)
  implementation(libs.prefs.core)
  implementation(libs.appCompat)
  implementation(libs.coroutines.core)
  implementation(libs.dagger.core)
  implementation(libs.timber)
  implementation(libs.picasso)
  implementation(libs.serialization.json)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)

  testImplementation(libs.truth)
  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.mockk)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
}
