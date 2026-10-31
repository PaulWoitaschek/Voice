plugins {
  id("voice.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.featureflag)
  api(projects.core.datastore)

  implementation(libs.serialization.json)

  testImplementation(libs.androidxCore)
}
