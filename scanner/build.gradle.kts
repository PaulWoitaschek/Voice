plugins {
  id("voice.library")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
  generateDaggerFactoriesOnly.set(true)
}

dependencies {
  implementation(projects.data)
  implementation(projects.common)
  implementation(projects.pref)

  implementation(libs.appCompat)
  implementation(libs.dagger.core)
  implementation(libs.serialization.json)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)
  implementation(libs.datastore)
  implementation(libs.media3.exoplayer)
  implementation(libs.coroutines.guava)

  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.mockk)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
}
