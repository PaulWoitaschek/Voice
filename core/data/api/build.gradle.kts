plugins {
  id("voice.library")
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  explicitApi()
}

dependencies {
  api(projects.core.common)
  api(projects.core.documentfile)
  implementation(libs.metro.runtime)
  implementation(libs.appCompat)
  implementation(libs.androidxCore)
  implementation(libs.serialization.json)

  // https://issuetracker.google.com/issues/209688774
  api(libs.compose.runtime)

  api(libs.room.runtime)

  api(libs.datastore)

  testImplementation(libs.room.testing)
}
