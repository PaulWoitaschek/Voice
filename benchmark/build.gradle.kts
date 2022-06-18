plugins {
  id("voice.library")
  alias(libs.plugins.benchmark)
}

android {

  defaultConfig {
    testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
  }
}

dependencies {
  implementation(projects.data)
  androidTestImplementation(libs.benchmark)
  androidTestImplementation(libs.androidX.test.runner)
  androidTestImplementation(libs.androidX.test.junit)
  androidTestImplementation(libs.androidX.test.core)
}
