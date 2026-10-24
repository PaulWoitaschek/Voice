plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

dependencies {
  api(libs.datastore)
  api(libs.serialization.json)
  implementation(libs.androidxCore)
}
