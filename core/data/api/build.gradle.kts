plugins {
  id("voice.library")
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.metro)
}

kotlin {
  explicitApi()
}

dependencies {
  api(projects.core.common)
  api(projects.core.documentfile)
  api(projects.core.datastore)
  implementation(libs.appCompat)
  implementation(libs.androidxCore)
  implementation(libs.serialization.json)

  // https://issuetracker.google.com/issues/209688774
  api(libs.compose.runtime)

  api(libs.room.runtime)

  testImplementation(libs.room.testing)
}
