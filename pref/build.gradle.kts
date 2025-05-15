plugins {
  id("voice.library")
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  implementation(libs.androidxCore)

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
}
