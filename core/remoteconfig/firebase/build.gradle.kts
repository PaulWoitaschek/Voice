plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(projects.core.remoteconfig.api)
  implementation(libs.firebase.remoteconfig)
  implementation(libs.coroutines.playServices)
}
