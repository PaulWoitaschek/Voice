plugins {
  id("voice.library")
  id("kotlin-parcelize")
  id("kotlin-kapt")
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.anvil)
  alias(libs.plugins.ksp)
}

anvil {
  generateDaggerFactories.set(true)
}

ksp {
  arg("room.schemaLocation", "$projectDir/schemas")
  allWarningsAsErrors = providers.gradleProperty("voice.warningsAsErrors").get().toBooleanStrict()

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
  implementation(projects.pref)

  api(libs.room.runtime)
  ksp(libs.room.compiler)

  implementation(libs.dagger.core)
  kaptTest(libs.dagger.compiler)
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
