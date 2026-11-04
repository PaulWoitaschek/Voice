plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.remoteconfig.api)
  implementation(libs.firebase.remoteconfig)
}
