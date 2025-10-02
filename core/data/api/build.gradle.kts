plugins {
  id("voice.library")
  id("kotlin-parcelize")
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  explicitApi()
}

dependencies {
  api(projects.core.common)
  api(projects.core.documentfile)
  implementation(libs.metro.runtime)
  implementation(libs.appCompat)
  implementation(libs.androidxCore)
  implementation(libs.serialization.json)

  api(libs.room.runtime)

  implementation(libs.datastore)

  testImplementation(libs.room.testing)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
  testImplementation(libs.coroutines.test)
}
