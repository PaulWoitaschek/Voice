plugins {
  id("voice.library")
  id("voice.compose")
  id("kotlin-parcelize")
  alias(libs.plugins.metro)
  alias(libs.plugins.kotlin.serialization)
}

metro {
  interop {
    includeDagger()
  }
}

android {
  androidResources {
    enable = true
  }
}

dependencies {
  implementation(projects.strings)

  implementation(libs.appCompat)
  implementation(libs.dagger.core)
  implementation(libs.material)
  api(libs.immutable)
  api(libs.conductor)
  api(libs.datastore)
  implementation(libs.androidxCore)
  implementation(libs.viewBinding)
  implementation(libs.serialization.json)

  testImplementation(libs.junit)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.robolectric)
}
