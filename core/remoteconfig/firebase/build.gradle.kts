plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.remoteconfig.api)
  implementation(libs.firebase.remoteconfig)
}
