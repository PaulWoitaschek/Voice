plugins {
  alias(libs.plugins.kotlin.jvm)
  id("voice.ktlint")
  application
}

application {
  mainClass.set("voice.scripts.ScriptKt")
}

dependencies {
  implementation(libs.clikt)
}

kotlin {
  jvmToolchain {
    languageVersion.set(JavaLanguageVersion.of(17))
  }
}
