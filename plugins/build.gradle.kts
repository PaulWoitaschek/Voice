plugins {
  `kotlin-dsl`
  alias(libs.plugins.spotless)
}

dependencies {
  implementation(libs.androidPluginForGradle)
  implementation(libs.kotlin.pluginForGradle)
  implementation(libs.kotlin.powerAssert)
  implementation(libs.compose.compiler.gradle.plugin)
  implementation(libs.spotless.gradlePlugin)
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
    create("ktfmt") {
      id = "voice.ktfmt"
      implementationClass = "KtfmtPlugin"
    }
  }
}

spotless {
  kotlin {
    target("src/**/*.kt")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
  }
  kotlinGradle {
    target("*.kts")
    ktfmt(libs.versions.ktfmt.get()).googleStyle()
  }
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(libs.versions.jvm.toolchain.get().toInt()))
  }
}
