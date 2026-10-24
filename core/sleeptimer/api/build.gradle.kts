plugins {
  id("voice.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  implementation(projects.core.common)
  api(projects.core.datastore)
}
