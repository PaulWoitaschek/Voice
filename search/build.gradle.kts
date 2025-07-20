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
  implementation(projects.data)
  implementation(libs.dagger.core)
}
