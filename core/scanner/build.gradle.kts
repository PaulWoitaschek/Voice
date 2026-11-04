plugins {
  id("voice.library")
}

kotlin {
  explicitApi()
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.documentfile)
  implementation(projects.core.initializer)

  implementation(libs.slf4j.noop)
  implementation(libs.jebml)
  implementation(libs.media3.inspector)
  implementation(libs.coroutines.guava)

  testImplementation(projects.core.data.impl)
}
