plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.data)
  implementation(projects.strings)
  implementation(libs.dagger.core)

  implementation(libs.bundles.retrofit)
  implementation(libs.paging.compose)
  implementation(libs.paging.runtime)

  implementation(libs.serialization.json)
  testImplementation(libs.bundles.testing.jvm)
}
