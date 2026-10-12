plugins {
  id("voice.library")
  id("voice.compose")
}

dependencies {
  api(libs.composablePreviewScanner)
  api(libs.compose.ui.testJunit)
  api(libs.robolectric)
  api(libs.androidX.test.core)
  api(libs.androidX.test.junit)
  api(libs.junit)
}
