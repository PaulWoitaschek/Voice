plugins {
  id("voice.library")
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
      assets.directories += "schemas"
    }
  }
}

dependencies {
  api(projects.core.data.api)
  implementation(projects.core.common)
  api(projects.core.documentfile)
  api(projects.core.featureflag)

  api(libs.datastore)
  implementation(libs.androidxCore)
  implementation(libs.serialization.json)
  api(projects.core.analytics.api)

  api(libs.room.runtime)
  ksp(libs.room.compiler)

  testImplementation(libs.room.testing)
}
