plugins {
  id("voice.library")
}

dependencies {
  implementation(libs.coroutines.core)
  implementation(libs.ffmpeg)
  implementation(libs.androidxCore)
}
