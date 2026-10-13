plugins {
  id("voice.library")
  alias(libs.plugins.compose.compiler)
  alias(libs.plugins.kotlin.serialization)
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(projects.core.common)
  api(projects.core.documentfile)
  api(libs.metro.runtime)
  implementation(libs.androidxCore)
  api(libs.serialization.json)

  // https://issuetracker.google.com/issues/209688774
  api(libs.compose.runtime)

  api(libs.room.runtime)

  api(libs.datastore)

  testImplementation(libs.room.testing)
}
