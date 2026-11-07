plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.scanner)

  implementation(libs.bundles.retrofit)
  implementation(libs.okhttp)
  implementation(libs.paging.compose)
  implementation(libs.paging.runtime)
  implementation(libs.navigation3.ui)

  testImplementation(libs.okhttp.mockwebserver)
}
