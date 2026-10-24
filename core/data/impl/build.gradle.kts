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

  sourceSets {
    named("test") {
      assets.directories += "schemas"
    }
  }
}

dependencies {
  api(projects.core.data.api)
  api(projects.core.common)
  api(projects.core.documentfile)
  implementation(projects.core.datastore)

  implementation(libs.datastore)
  implementation(libs.androidxCore)
  implementation(libs.serialization.json)
  implementation(libs.coroutines.core)
  implementation(projects.core.analytics.api)

  api(libs.room.runtime)
  ksp(libs.room.compiler)

  testImplementation(libs.room.testing)
}
