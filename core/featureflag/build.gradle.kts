plugins {
  id("voice.library")
}

dependencies {
  implementation(projects.core.datastore)
  implementation(projects.core.remoteconfig.api)
  implementation(libs.datastore)
}
