plugins {
  id("voice.library")
  id("voice.compose")
}

dependencies {
  implementation(projects.core.strings)

  implementation(libs.material)

  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.robolectric)
}
