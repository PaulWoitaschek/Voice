plugins {
  id("voice.library")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.data)
  implementation(libs.dagger.core)
}
