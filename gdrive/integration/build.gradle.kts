plugins {
  id("voice.library")
  id("com.google.devtools.ksp")
}

dependencies {
  api(libs.google.api.client.android)
  api(libs.google.api.services.drive)
  implementation(libs.google.oauth.client.jetty)

  implementation(projects.common)
  implementation(projects.data)

  implementation(libs.coroutines.core)
  implementation(libs.dagger.core)
  ksp(libs.dagger.compiler) // Make sure this is defined in libs.versions.toml if ksp plugin is applied at top-level build.gradle/settings.gradle

  testImplementation(libs.bundles.testing.jvm) // Corrected bundle name
}
