plugins {
  id("voice.library")
  alias(libs.plugins.metro)
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
  implementation(libs.androidxCore)
  implementation(libs.media3.inspector)
  implementation(libs.coroutines.guava)

  testImplementation(projects.core.data.impl)
}
