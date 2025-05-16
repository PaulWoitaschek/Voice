plugins {
  id("voice.library")
}

dependencies {
  implementation(libs.androidxCore)
  api(libs.datastore)

  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
}
