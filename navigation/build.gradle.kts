plugins {
  id("voice.library")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

dependencies {
  api(libs.navigation3.runtime)
  api(projects.core.data.api)
  implementation(projects.core.common)
  implementation(libs.compose.material3)
  testImplementation(kotlin("reflect"))
  testImplementation(libs.serialization.json)
}

kotlin{
  compilerOptions {
    optIn.add("androidx.compose.material3.ExperimentalMaterial3Api")
  }
}
