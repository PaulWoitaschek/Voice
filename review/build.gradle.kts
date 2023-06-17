plugins {
  id("voice.library")
  id("voice.compose")
}

android {
  buildFeatures {
    androidResources = true
  }
}

dependencies {
  implementation(projects.strings)
  implementation(libs.review)
  implementation(libs.lottie)
}
