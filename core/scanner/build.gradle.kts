plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

kotlin {
  explicitApi()
}

dependencies {
  api(projects.core.data.api)
  api(projects.core.documentfile)
  api(projects.core.initializer)

  runtimeOnly(libs.slf4j.noop)
  implementation(libs.jebml)
  implementation(libs.androidxCore)
  implementation(libs.documentFile)
  implementation(libs.media3.inspector)
  implementation(libs.coroutines.guava)

  testImplementation(projects.core.data.impl)
  testImplementation(libs.room.runtime)
}
