plugins {
  id("voice.library")
  alias(libs.plugins.anvil)

}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.common)
  implementation(libs.dagger.core)
  implementation(libs.androidxCore)
}
