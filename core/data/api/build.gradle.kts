plugins {
  id("voice.library")
  alias(libs.plugins.compose.compiler)
}

kotlin {
  explicitApi()
}

dependencies {
  api(projects.core.common)
  api(projects.core.documentfile)
  api(projects.core.datastore)
  implementation(libs.appCompat)

  // https://issuetracker.google.com/issues/209688774
  api(libs.compose.runtime)

  api(libs.room.runtime)

  testImplementation(libs.room.testing)
}
