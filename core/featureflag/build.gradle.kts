plugins {
  id("voice.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.remoteconfig.api)
  implementation(libs.datastore)
  implementation(libs.serialization.json)
}
