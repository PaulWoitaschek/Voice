plugins {
  `kotlin-dsl`
}

dependencies {
  implementation(libs.androidPluginForGradle)
  implementation(libs.kotlin.pluginForGradle)
  implementation(libs.ktlint.gradlePlugin)
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
    create("ktlint") {
      id = "voice.ktlint"
      implementationClass = "KtlintPlugin"
    }
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}
