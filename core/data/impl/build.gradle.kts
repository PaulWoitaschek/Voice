plugins {
  id("voice.library")
  id("kotlin-parcelize")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
  alias(libs.plugins.ksp)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  allWarningsAsErrors = providers.gradleProperty("voice.warningsAsErrors").get().toBooleanStrict()
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
  api(projects.core.data.api)
  api(projects.core.common)
  api(projects.core.documentfile)

  implementation(libs.datastore)
  implementation(libs.androidxCore)
  implementation(libs.serialization.json)
  implementation(libs.coroutines.core)

  api(libs.room.runtime)
  ksp(libs.room.compiler)

  testImplementation(libs.room.testing)
  testImplementation(libs.androidX.test.core)
  testImplementation(libs.androidX.test.junit)
  testImplementation(libs.androidX.test.runner)
  testImplementation(libs.junit)
  testImplementation(libs.robolectric)
  testImplementation(libs.koTest.assert)
  testImplementation(libs.coroutines.test)
}
