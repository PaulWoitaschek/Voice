plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

metro {
  interop {
    includeDagger()
    includeAnvil()
  }
}

dependencies {
  implementation(projects.common)
  implementation(libs.dagger.core)
  implementation(libs.androidxCore)
}
