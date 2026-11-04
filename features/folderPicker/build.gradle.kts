plugins {
  id("voice.feature")
}

dependencies {
  implementation(projects.core.data.api)
  implementation(projects.core.documentfile)
  implementation(projects.core.scanner)
  implementation(projects.core.featureflag)
  implementation(projects.core.kioskmode)
  implementation(projects.core.analytics.api)
  implementation(projects.core.audiobookshelf.api)

  implementation(libs.coil)
  implementation(libs.documentFile)
}
