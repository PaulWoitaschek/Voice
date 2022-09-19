plugins {
  id("voice.library")
  id("voice.compose")
  alias(libs.plugins.anvil)
}

anvil {
  generateDaggerFactories.set(true)
}

android {
  buildFeatures {
    viewBinding = true
  }
}

dependencies {
  implementation(projects.common)
  implementation(libs.material)
  implementation(libs.compose.snapper)
}
