plugins {
  id("voice-android-library")
}

dependencies {
  implementation(libs.coroutines.core)
  implementation(libs.ffmpeg)
  implementation(libs.androidxCore)
  implementation(libs.timber)
}
