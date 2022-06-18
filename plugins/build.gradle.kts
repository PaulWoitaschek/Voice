plugins {
  alias(libs.plugins.kotlin.jvm)
  `java-gradle-plugin`
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

gradlePlugin {
  plugins {
    create("library") {
      id = "voice.library"
      implementationClass = "LibraryPlugin"
    }
    create("app") {
      id = "voice.app"
      implementationClass = "AppPlugin"
    }
  }
}
