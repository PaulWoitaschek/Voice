plugins {
  id("voice.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(projects.core.common)
  api(projects.core.data.api)
  api(projects.core.remoteconfig.api)
  api(libs.datastore)
  api(libs.serialization.json)
}
