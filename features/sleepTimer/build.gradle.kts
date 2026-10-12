plugins {
  id("voice.library")
  id("voice.compose")
}

dependencies {
  implementation(projects.core.strings)
  implementation(projects.core.ui)
}
