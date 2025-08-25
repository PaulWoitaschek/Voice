plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(projects.data.api)
  implementation(projects.common)

  implementation(libs.slf4j.noop)
  implementation(libs.jebml)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)
  implementation(libs.media3.exoplayer)
  implementation(libs.coroutines.guava)

  testImplementation(projects.data.impl)
  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.mockk)
  testImplementation(libs.coroutines.test)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
}
