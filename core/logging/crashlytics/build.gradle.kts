plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.initializer)
  implementation(libs.firebase.crashlytics)
}
