plugins {
  `kotlin-dsl`
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
    create("compose") {
      id = "voice.compose"
      implementationClass = "ComposePlugin"
    }
  }
}

kotlin {
  jvmToolchain {
    (this as JavaToolchainSpec).languageVersion.set(JavaLanguageVersion.of(17))
  }
}
