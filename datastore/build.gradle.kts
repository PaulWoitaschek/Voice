plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(libs.serialization.json)
  api(libs.datastore)
}
