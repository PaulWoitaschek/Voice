plugins {
  id("voice.library")
}

dependencies {
  implementation(libs.androidxCore)

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
}
