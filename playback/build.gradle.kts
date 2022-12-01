plugins {
  id("voice.library")
  alias(libs.plugins.anvil)
  alias(libs.plugins.kotlin.serialization)

}

anvil {
  generateDaggerFactories.set(true)
}

dependencies {
  implementation(projects.common)
  implementation(projects.strings)
  implementation(projects.data)

  implementation(libs.media)
  implementation(libs.androidxCore)
  implementation(libs.prefs.core)
  implementation(libs.datastore)
  implementation(libs.coil)
  implementation(libs.coroutines.guava)
  implementation(libs.serialization.json)
  implementation(libs.dagger.core)

  implementation(libs.media3.exoplayer)
  implementation(libs.media3.session)

  testImplementation(libs.bundles.testing.jvm)
}
