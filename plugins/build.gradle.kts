plugins {
  `kotlin-dsl`
  `kotlin-dsl-precompiled-script-plugins`
}

repositories {
  google()
  mavenCentral()
}

dependencies {
  implementation(libs.androidPluginForGradle)
  implementation(libs.kotlin.pluginForGradle)
  implementation(libs.kotlin.compilerEmbeddable)
}
