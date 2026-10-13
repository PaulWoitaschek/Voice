plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

dependencies {
  implementation(projects.core.common)
  api(projects.core.data.api)
  api(projects.navigation)
  api(projects.core.scanner)
  implementation(projects.core.strings)
  implementation(projects.core.ui)
  api(projects.core.featureflag)

  api(libs.retrofit.core)
  implementation(libs.retrofit.scalars)
  implementation(libs.retrofit.serialization)
  api(libs.okhttp)
  implementation(libs.okhttp.coroutines)
  implementation(libs.paging.compose)
  implementation(libs.paging.runtime)
  implementation(libs.navigation3.ui)

  api(libs.serialization.json)
  testImplementation(projects.core.data.impl)
  testImplementation(projects.core.remoteconfig.noop)
}
