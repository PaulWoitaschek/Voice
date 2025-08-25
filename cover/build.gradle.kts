plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.data.api)
  implementation(projects.strings)
  implementation(projects.remoteconfig.core)

  implementation(libs.bundles.retrofit)
  implementation(libs.okhttp)
  implementation(libs.paging.compose)
  implementation(libs.paging.runtime)

  implementation(libs.serialization.json)
  testImplementation(projects.data.impl)
  testImplementation(libs.bundles.testing.jvm)
  testImplementation(projects.remoteconfig.noop)
}
