plugins {
  id("voice.library")
  id("voice.compose")
  id("kotlin-parcelize")
  alias(libs.plugins.anvil)
  alias(libs.plugins.kotlin.serialization)
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.strings)
  implementation(libs.appCompat)
  implementation(libs.dagger.core)
  implementation(libs.material)
  api(libs.immutable)
  implementation(libs.prefs.core)
  api(libs.conductor.core)
  implementation(libs.androidxCore)
  implementation(libs.viewBinding)
  implementation(libs.serialization.json)

  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.robolectric)
}
