plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.strings)
  implementation(libs.appCompat)
  implementation(libs.dagger.core)
  implementation(libs.material)
  implementation(libs.prefs.core)
  api(libs.conductor.core)
  implementation(libs.androidxCore)
  implementation(libs.viewBinding)

  testImplementation(libs.truth)
  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.robolectric)
}
