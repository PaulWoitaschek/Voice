plugins {
  id("voice.library")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(libs.dagger.core)
  api(libs.serialization.json)
  api(libs.datastore)
}
