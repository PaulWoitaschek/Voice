plugins {
  id("voice.library")
  id("kotlin-parcelize")
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  explicitApi()
}

android {

  defaultConfig {
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  sourceSets {
    named("test") {
      assets.srcDir(project.file("schemas"))
    }
  }
}

dependencies {
  api(projects.common)
  api(projects.documentfile)
  implementation(libs.appCompat)
  implementation(libs.androidxCore)
  implementation(libs.serialization.json)

  api(libs.room.runtime)

  implementation(libs.datastore)
  implementation(libs.documentFile)

  testImplementation(libs.room.testing)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
  testImplementation(libs.coroutines.test)
}
