plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.playback)
  implementation(projects.data)
  implementation(projects.datastore)
  implementation(libs.dagger.core)
  implementation(libs.materialDialog.core)
  implementation(libs.materialDialog.input)
}
