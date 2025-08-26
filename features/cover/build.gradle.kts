plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.navigation)
  implementation(projects.core.strings)
  implementation(projects.core.remoteconfig.core)

  implementation(libs.bundles.retrofit)
  implementation(libs.okhttp)
  implementation(libs.paging.compose)
  implementation(libs.paging.runtime)

  implementation(libs.serialization.json)
  testImplementation(projects.core.data.impl)
  testImplementation(libs.bundles.testing.jvm)
  testImplementation(projects.core.remoteconfig.noop)
}
