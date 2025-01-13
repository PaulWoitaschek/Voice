plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

android.buildFeatures.androidResources = true

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.sleepTimer)
  implementation(projects.documentfile)
  implementation(projects.pref)

  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.materialDialog.core)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)

  testImplementation(libs.molecule)

  implementation(libs.dagger.core)
}
