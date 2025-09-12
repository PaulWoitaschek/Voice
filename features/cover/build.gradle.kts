plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

android {
  androidResources.enable = true
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.navigation)
  implementation(projects.core.scanner)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  implementation(projects.core.remoteconfig.api)

  implementation(libs.bundles.retrofit)
  implementation(libs.okhttp)
  implementation(libs.paging.compose)
  implementation(libs.paging.runtime)
  implementation(libs.navigation3.ui)

  implementation(libs.serialization.json)
  testImplementation(projects.core.data.impl)
  testImplementation(libs.bundles.testing.jvm)
  testImplementation(projects.core.remoteconfig.noop)
}
