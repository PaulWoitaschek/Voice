plugins {
  id("voice.library")
  alias(libs.plugins.metro)
}

metro {
  interop {
    includeDagger()
  }
}

dependencies {
  implementation(projects.strings)
  implementation(projects.datastore)
  implementation(projects.common)
  implementation(projects.data)
  implementation(projects.playback)
  implementation(projects.scanner)
  implementation(projects.playbackScreen)
  implementation(projects.sleepTimer)
  implementation(projects.settings)
  implementation(projects.folderPicker)
  implementation(projects.bookOverview)
  implementation(projects.migration)
  implementation(projects.search)
  implementation(projects.cover)
  implementation(projects.documentfile)
  implementation(projects.onboarding)
  implementation(projects.bookmark)
}
