plugins {
  id("voice.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(libs.navigation3.runtime)
  api(projects.data.api)
}
