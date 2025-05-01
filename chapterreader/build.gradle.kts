plugins {
  id("voice.library")
  alias(libs.plugins.anvil)
}

dependencies{
  implementation(projects.logging.core)
  implementation(libs.dagger.core)
  implementation(libs.jebml)
  implementation(libs.slf4j.noop)
}
