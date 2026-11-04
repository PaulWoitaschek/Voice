plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.initializer)
  implementation(libs.firebase.crashlytics)
}
